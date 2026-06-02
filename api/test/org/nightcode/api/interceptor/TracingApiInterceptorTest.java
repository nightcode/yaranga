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

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.nightcode.api.ApiCall;
import org.nightcode.api.ApiContext;
import org.nightcode.api.ApiPipe;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.common.pool.SessionPool;

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
 * Unit test for {@link TracingApiInterceptorTest}.
 */
public class TracingApiInterceptorTest {

  private TracingTestUtils.Harness harness;

  @BeforeEach void setUp() {
    harness = TracingTestUtils.setup();
  }

  @AfterEach void tearDown() {
    harness.close();
  }

  private ApiContext<Object> stubContext(String serviceName, ApiCall<Request, Response> delegate) {
    return new ApiContext<>() {
      @Override public SessionPool<Object, ApiPipe<Object>> connectionPool() {
        return null;
      }

      @Override public long executeTimeoutMs() {
        return 0;
      }

      @Override public int maxAttempts() {
        return 0;
      }

      @Override public Metadata metadata() {
        return Metadata.getDefaultInstance();
      }

      @Override public <Q extends Message, R extends Message> ApiCall<Q, R> newApiCall(Class<Q> q, Class<R> r) {
        // noinspection unchecked
        return (ApiCall<Q, R>) delegate;
      }

      @Override public <M extends Message> Any packMessage(M message) {
        return null;
      }

      @Override public String serviceName() {
        return serviceName;
      }
    };
  }

  private ApiCall<Request, Response> delegate() {
    return (msg, metadata) -> CompletableFuture.completedFuture(Response.newBuilder().setService("TestService").build());
  }
  
  @Test void createsClientSpanWithRpcAttributes() throws Exception {
    AtomicReference<Metadata> seen = new AtomicReference<>();
    ApiCall<Request, Response> delegate = (msg, metadata) -> {
      seen.set(metadata);
      return CompletableFuture.completedFuture(Response.getDefaultInstance());
    };
    
    new TracingApiInterceptor(harness.tracer())
        .intercept(stubContext("TestService", delegate), Request.class, Response.class)
        .executeAsync(Request.getDefaultInstance(), Metadata.getDefaultInstance())
        .get();

    Metadata metadata = seen.get();
    assertTrue(metadata.hasTrace());
    assertTrue(metadata.getTrace().getContextMap().containsKey("traceparent"));
    
    List<SpanData> spans = harness.exporter().getFinishedSpanItems();
    assertEquals(1, spans.size(), "expected exactly one span");

    SpanData spanData = spans.getFirst();
    assertEquals(SpanKind.CLIENT, spanData.getKind());
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

  @Test void parentSpanIsRecordedWhenCallerHasOne() throws Exception {
    Span parent = harness.tracer().spanBuilder("parent").startSpan();
    try (var ignored = parent.makeCurrent()) {
      new TracingApiInterceptor(harness.tracer())
          .intercept(stubContext("TestService", delegate()), Request.class, Response.class)
          .executeAsync(Request.getDefaultInstance(), Metadata.getDefaultInstance())
          .get();
    } finally {
      parent.end();
    }

    List<SpanData> spans      = harness.exporter().getFinishedSpanItems();
    SpanData       rpcSpan    = spans.stream().filter(s -> s.getKind() == SpanKind.CLIENT).findFirst().orElseThrow();
    SpanData       parentSpan = spans.stream().filter(s -> s.getKind() == SpanKind.INTERNAL).findFirst().orElseThrow();
    assertEquals(parentSpan.getSpanId(), rpcSpan.getParentSpanId());
    assertEquals(parentSpan.getTraceId(), rpcSpan.getTraceId());
  }

  @Test void recordsAsyncFailure() {
    RuntimeException cause = new RuntimeException();

    CompletableFuture<Response> cf = new TracingApiInterceptor(harness.tracer())
        .intercept(stubContext("TestService", (m, md) -> CompletableFuture.failedFuture(cause)), Request.class, Response.class)
        .executeAsync(Request.getDefaultInstance(), Metadata.getDefaultInstance());

    ExecutionException ex = assertThrows(ExecutionException.class, cf::get);
    assertSame(cause, ex.getCause());

    SpanData spanData = harness.exporter().getFinishedSpanItems().getFirst();
    assertEquals(StatusCode.ERROR, spanData.getStatus().getStatusCode());
    assertEquals(1, spanData.getEvents().size());
    assertEquals("exception", spanData.getEvents().getFirst().getName());
  }

  @Test void recordsSyncThrowAndRethrows() {
    RuntimeException           cause    = new RuntimeException();
    ApiCall<Request, Response> delegate = (msg, metadata) -> {
      throw cause;
    };

    ApiCall<Request, Response> call = new TracingApiInterceptor(harness.tracer())
        .intercept(stubContext("TestService", delegate), Request.class, Response.class);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> call.executeAsync(Request.getDefaultInstance(), Metadata.getDefaultInstance()));
    assertSame(cause, ex);

    SpanData spanData = harness.exporter().getFinishedSpanItems().getFirst();
    assertEquals(StatusCode.ERROR, spanData.getStatus().getStatusCode());
    assertFalse(spanData.getEvents().isEmpty());
  }
}
