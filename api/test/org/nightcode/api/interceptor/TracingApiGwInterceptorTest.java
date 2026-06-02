/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nightcode.api.interceptor;

import com.google.protobuf.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.nightcode.api.ApiGwCall;
import org.nightcode.api.ApiGwContext;
import org.nightcode.api.ApiHandler;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.api.message.Trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nightcode.api.ApiInterceptor.API_METHOD;
import static org.nightcode.api.ApiInterceptor.API_SERVICE;

/**
 * Unit test for {@link TracingApiGwInterceptorTest}.
 */
public class TracingApiGwInterceptorTest {

  private TracingTestUtils.Harness harness;

  @BeforeEach void setUp() {
    harness = TracingTestUtils.setup();
  }

  @AfterEach void tearDown() {
    harness.close();
  }

  private ApiGwContext stubGwContext(ApiGwCall<Request, Response> delegate) {
    return new ApiGwContext() {
      @Override public void close() {
        // do nothing
      }

      @Override public int maxBodyLengthBytes() {
        return 0;
      }

      @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> newApiCall() {
        // noinspection unchecked
        return (ApiGwCall<Q, R>) delegate;
      }

      @Override public ConcurrentMap<String, ApiHandler> apiHandlers() {
        return new ConcurrentHashMap<>();
      }
    };
  }

  private ApiGwCall<Request, Response> delegate() {
    return (sn, mh, msg, metadata) -> CompletableFuture.completedFuture(Response.getDefaultInstance());
  }
  
  @Test void createsServerSpanWithRpcAttributes() throws Exception {
    AtomicReference<Metadata> seen = new AtomicReference<>();
    ApiGwCall<Request, Response> delegate = (sn, mh, msg, metadata) -> {
      seen.set(metadata);
      return CompletableFuture.completedFuture(Response.getDefaultInstance());
    };
    
    new TracingApiGwInterceptor(harness.tracer())
        .intercept(stubGwContext(delegate))
        .executeAsync("TestService", r -> null, Request.getDefaultInstance(), Metadata.getDefaultInstance())
        .get();

    Metadata metadata = seen.get();
    assertTrue(metadata.hasTrace());
    assertTrue(metadata.getTrace().getContextMap().containsKey("traceparent"));

    List<SpanData> spans = harness.exporter().getFinishedSpanItems();
    assertEquals(1, spans.size(), "expected exactly one span");

    SpanData spanData = spans.getFirst();
    assertEquals(SpanKind.SERVER, spanData.getKind());
    assertEquals(Request.getDescriptor().getFullName(), spanData.getName());
    assertEquals("TestService", spanData.getAttributes().get(API_SERVICE));
    assertEquals("Request", spanData.getAttributes().get(API_METHOD));
    assertEquals(StatusCode.UNSET, spanData.getStatus().getStatusCode());
    assertTrue(spanData.getEvents().isEmpty());

    String   traceParent = metadata.getTrace().getContextMap().get("traceparent");
    // format: 00-<trace-id>-<span-id>-<flags>
    String[] parts = traceParent.split("-");
    assertEquals(spanData.getTraceId(), parts[1]);
    assertEquals(spanData.getSpanId(), parts[2]);
  }

  @Test void linksToRemoteParentWhenMetadataHasTrace() throws Exception {
    Span          remoteParent = harness.tracer().spanBuilder("remote").startSpan();
    Trace.Builder traceBuilder = Trace.newBuilder();
    try (var ignored = remoteParent.makeCurrent()) {
      GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), traceBuilder, TraceTextMapSetter.INSTANCE);
    }
    remoteParent.end();
    Metadata incoming = Metadata.newBuilder().setTrace(traceBuilder.build()).build();

    new TracingApiGwInterceptor(harness.tracer())
        .intercept(stubGwContext(delegate()))
        .executeAsync("TestService", r -> null, Request.getDefaultInstance(), incoming)
        .get();

    SpanData remote = harness.exporter().getFinishedSpanItems().stream().filter(s -> s.getKind() == SpanKind.INTERNAL).findFirst().orElseThrow();
    SpanData server = harness.exporter().getFinishedSpanItems().stream().filter(s -> s.getKind() == SpanKind.SERVER).findFirst().orElseThrow();
    assertEquals(remote.getTraceId(), server.getTraceId(), "trace id must be preserved");
    assertEquals(remote.getSpanId(), server.getParentSpanId(), "remote span must be parent");
  }

  @Test void startsRootTraceWhenNoIncomingMetadata() throws Exception {
    new TracingApiGwInterceptor(harness.tracer())
        .intercept(stubGwContext(delegate()))
        .executeAsync("TestService", r -> null, Request.getDefaultInstance(), Metadata.getDefaultInstance())
        .get();

    SpanData spanData = harness.exporter().getFinishedSpanItems().getFirst();
    assertEquals(SpanKind.SERVER, spanData.getKind());
    assertFalse(spanData.getParentSpanContext().isValid(), "server span must be root");
  }

  @Test void recordsAsyncFailure() {
    RuntimeException             cause    = new RuntimeException();
    ApiGwCall<Request, Response> delegate = (sn, mh, msg, metadata) -> CompletableFuture.failedFuture(cause);

    CompletableFuture<Response> cf = new TracingApiGwInterceptor(harness.tracer())
        .<Request, Response>intercept(stubGwContext(delegate))
        .executeAsync("TestService", r -> null, Request.getDefaultInstance(), Metadata.getDefaultInstance());

    ExecutionException ex = assertThrows(ExecutionException.class, cf::get);
    assertSame(cause, ex.getCause());

    SpanData spanData = harness.exporter().getFinishedSpanItems().getFirst();
    assertEquals(StatusCode.ERROR, spanData.getStatus().getStatusCode());
    assertEquals(1, spanData.getEvents().size());
  }

  @Test void recordsSyncFailure() {
    RuntimeException             cause    = new RuntimeException();
    ApiGwCall<Request, Response> delegate = (sn, mh, msg, metadata) -> {
      throw cause;
    };

    ApiGwCall<Request, Response> call = new TracingApiGwInterceptor(harness.tracer()).intercept(stubGwContext(delegate));

    RuntimeException ex = assertThrows(RuntimeException.class, () -> call.executeAsync("TestService", r -> null, Request.getDefaultInstance(), Metadata.getDefaultInstance()));
    assertSame(cause, ex);

    SpanData spanData = harness.exporter().getFinishedSpanItems().getFirst();
    assertEquals(StatusCode.ERROR, spanData.getStatus().getStatusCode());
    assertEquals(1, spanData.getEvents().size());
  }

  @Test void overwritesIncomingTraceWithFreshContext() throws Exception {
    Metadata                  incoming = Metadata.newBuilder().setTrace(Trace.newBuilder().putContext("foo", "bar")).build();
    AtomicReference<Metadata> seen     = new AtomicReference<>();
    ApiGwCall<Request, Response> delegate = (sn, mh, msg, metadata) -> {
      seen.set(metadata);
      return CompletableFuture.completedFuture(Response.getDefaultInstance());
    };

    new TracingApiGwInterceptor(harness.tracer())
        .intercept(stubGwContext(delegate))
        .executeAsync("TestService", r -> null, Request.getDefaultInstance(), incoming)
        .get();

    Metadata metadata = seen.get();
    assertTrue(metadata.getTrace().getContextMap().containsKey("traceparent"));
    assertFalse(metadata.getTrace().getContextMap().containsKey("foo"));
  }
}
