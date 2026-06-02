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
import org.nightcode.api.ApiCall;
import org.nightcode.api.ApiContext;
import org.nightcode.api.ApiInterceptor;
import org.nightcode.api.SimpleApiCall;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Trace;
import org.nightcode.common.trace.opentelemetry.OtelTracerProvider;

/**
 * Tracing API interceptor.
 */
public class TracingApiInterceptor implements ApiInterceptor {

  private final Tracer tracer;

  public TracingApiInterceptor() {
    this(OtelTracerProvider.get(TracingApiInterceptor.class));
  }

  public TracingApiInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override public <A, Q extends Message, R extends Message> ApiCall<Q, R> intercept(ApiContext<A> clientApiContext,
                                                                                     Class<Q> requestClass,
                                                                                     Class<R> responseClass) {
    TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    String serviceName = clientApiContext.serviceName();
    String methodName  = requestClass.getSimpleName();

    return new SimpleApiCall<>(clientApiContext.newApiCall(requestClass, responseClass)) {
      @Override public CompletableFuture<R> executeAsync(Q message, Metadata metadata) {
        Span apiCallSpan = tracer.spanBuilder(message.getDescriptorForType().getFullName())
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(API_SERVICE, serviceName)
            .setAttribute(API_METHOD, methodName)
            .startSpan();

        try (Scope ignored = apiCallSpan.makeCurrent()) {
          Trace.Builder traceBuilder = Trace.newBuilder();
          propagator.inject(Context.current(), traceBuilder, TraceTextMapSetter.INSTANCE);

          Metadata enrichedMetadata = metadata.toBuilder().setTrace(traceBuilder.build()).build();

          CompletableFuture<R> cf;
          try {
            cf = super.executeAsync(message, enrichedMetadata);
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
    return "TracingApiInterceptor{tracer=" + tracer + '}';
  }
}
