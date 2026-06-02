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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.nightcode.api.ApiGwCall;
import org.nightcode.api.ApiGwContext;
import org.nightcode.api.ApiGwInterceptor;
import org.nightcode.api.MethodHandler;
import org.nightcode.api.SimpleApiGwCall;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Trace;
import org.nightcode.common.trace.opentelemetry.OtelTracerProvider;

import static org.nightcode.api.ApiInterceptor.API_METHOD;
import static org.nightcode.api.ApiInterceptor.API_SERVICE;

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
    TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    return new SimpleApiGwCall<>(context.newApiCall()) {
      @Override public CompletableFuture<R> executeAsync(String serviceName, MethodHandler<Q, R> methodHandler, Q message,
                                                         Metadata metadata) {
        Context parentContext = metadata.hasTrace()
            ? propagator.extract(Context.current(), metadata.getTrace(), TraceTextMapGetter.INSTANCE)
            : Context.current();

        Span apiCallSpan = tracer.spanBuilder(message.getDescriptorForType().getFullName())
            .setParent(parentContext)
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(API_SERVICE, serviceName)
            .setAttribute(API_METHOD, message.getClass().getSimpleName())
            .startSpan();

        try (Scope ignored = apiCallSpan.makeCurrent()) {
          Trace.Builder traceBuilder = Trace.newBuilder();
          propagator.inject(Context.current(), traceBuilder, TraceTextMapSetter.INSTANCE);

          Metadata enrichedMetadata = metadata.toBuilder().setTrace(traceBuilder.build()).build();

          CompletableFuture<R> cf;
          try {
            cf = super.executeAsync(serviceName, methodHandler, message, enrichedMetadata);
          } catch (Throwable t) {
            apiCallSpan.recordException(t);
            apiCallSpan.setStatus(StatusCode.ERROR);
            apiCallSpan.end();
            throw t;
          }
          cf.whenComplete((r, t) -> {
            if (t != null) {
              apiCallSpan.recordException(t);
              apiCallSpan.setStatus(StatusCode.ERROR);
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
