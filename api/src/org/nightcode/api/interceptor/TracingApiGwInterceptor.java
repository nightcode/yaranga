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

import java.util.concurrent.CompletableFuture;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.nightcode.api.ApiGwCall;
import org.nightcode.api.ApiGwContext;
import org.nightcode.api.ApiGwInterceptor;
import org.nightcode.api.MethodHandler;
import org.nightcode.api.SimpleApiGwCall;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Trace;
import org.nightcode.common.trace.opentelemetry.OtelTracerProvider;

/**
 * Tracing API gateway interceptor.
 */
public class TracingApiGwInterceptor implements ApiGwInterceptor {

  private final Tracer tracer;

  public TracingApiGwInterceptor() {
    this(OtelTracerProvider.get(TracingApiGwInterceptor.class));
  }

  public TracingApiGwInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> intercept(ApiGwContext context) {
    return new SimpleApiGwCall<>(context.newApiCall()) {
      @Override public CompletableFuture<R> executeAsync(String serviceName, MethodHandler<Q, R> methodHandler, Q message,
                                                         Metadata metadata) {
        SpanBuilder builder = tracer.spanBuilder(message.getDescriptorForType().getFullName())
            .setSpanKind(SpanKind.SERVER);

        if (metadata.hasTrace()) {
          SpanContext parentContext = SpanContext.createFromRemoteParent(metadata.getTrace().getTraceId()
              , metadata.getTrace().getSpanId(), TraceFlags.getSampled(), TraceState.getDefault());
          Span parentSpan = Span.wrap(parentContext);
          builder.setParent(Context.current().with(parentSpan));
        }

        Span apiCallSpan = builder.startSpan();
        try (Scope unused = apiCallSpan.makeCurrent()) {
          SpanContext spanContext = apiCallSpan.getSpanContext();

          Trace.Builder tcBuilder = Trace.newBuilder()
              .setTraceId(spanContext.getTraceId())
              .setSpanId(spanContext.getSpanId());
          if (metadata.hasTrace()) {
            tcBuilder.setParentId(metadata.getTrace().getSpanId());
          }

          metadata = metadata.toBuilder().setTrace(tcBuilder).build();

          CompletableFuture<R> cf = super.executeAsync(serviceName, methodHandler, message, metadata);
          cf.whenComplete((r, t) -> {
            if (t != null) {
              apiCallSpan.recordException(t);
              apiCallSpan.setStatus(StatusCode.ERROR, t.getClass().getSimpleName());
            }
            apiCallSpan.end();
          });
          return cf;
        }
      }
    };
  }

  @Override public String toString() {
    return "TracingApiGwInterceptor{tracer=" + tracer + '}';
  }
}
