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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.nightcode.api.ApiCall;
import org.nightcode.api.ApiContext;
import org.nightcode.api.ApiInterceptor;
import org.nightcode.api.SimpleApiCall;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Trace;
import org.nightcode.common.trace.opentelemetry.TracerProvider;

/**
 * Tracing API interceptor.
 */
public class TracingApiInterceptor implements ApiInterceptor {

  private final Tracer tracer;

  public TracingApiInterceptor() {
    this(TracerProvider.get(TracingApiInterceptor.class));
  }

  public TracingApiInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override public <A, Q extends Message, R extends Message> ApiCall<Q, R> intercept(ApiContext<A> clientApiContext,
                                                                                     Class<Q> requestClass,
                                                                                     Class<R> responseClass) {
    return new SimpleApiCall<>(clientApiContext.newApiCall(requestClass, responseClass)) {
      @Override public CompletableFuture<R> executeAsync(Q message, Metadata metadata) {
        Span rpcSpan = tracer.spanBuilder(message.getDescriptorForType().getFullName()).setSpanKind(SpanKind.CLIENT).startSpan();
        if (!rpcSpan.getSpanContext().isSampled()) {
          try {
            return super.executeAsync(message, metadata);
          } finally {
            rpcSpan.end();
          }
        }

        try (Scope unused = rpcSpan.makeCurrent()) {
          metadata = metadata.toBuilder()
              .setTrace(Trace.newBuilder()
                  .setTraceId(rpcSpan.getSpanContext().getTraceId())
                  .setSpanId(rpcSpan.getSpanContext().getSpanId()))
              .build();
          CompletableFuture<R> cf = super.executeAsync(message, metadata);
          cf.whenComplete((r, t) -> {
            if (t != null) {
              rpcSpan.recordException(t);
              rpcSpan.setStatus(StatusCode.ERROR, t.getClass().getSimpleName());
            }
            rpcSpan.end();
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
