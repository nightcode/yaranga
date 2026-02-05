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

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.nightcode.api.ApiGwCall;
import org.nightcode.api.ApiGwContext;
import org.nightcode.api.ApiGwInterceptor;
import org.nightcode.api.MethodHandler;
import org.nightcode.api.SimpleApiGwCall;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Trace;

/**
 * Tracing API gateway interceptor.
 */
public class TracingApiGwInterceptor implements ApiGwInterceptor {

  private final Tracer tracer;

  public TracingApiGwInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> intercept(ApiGwContext context) {
    return new SimpleApiGwCall<>(context.newApiCall()) {
      @Override public CompletableFuture<R> executeAsync(String serviceName, MethodHandler<Q, R> methodHandler, Q message,
                                                         Metadata metadata) {
        Span apiCallSpan;
        if (metadata.hasTrace()) {
          io.micrometer.tracing.TraceContext tc = tracer.traceContextBuilder()
              .traceId(metadata.getTrace().getTraceId())
              .spanId(metadata.getTrace().getSpanId())
              .sampled(Boolean.TRUE)
              .build();
          apiCallSpan = tracer.spanBuilder().setParent(tc).name(message.getDescriptorForType().getFullName()).start();
        } else {
          apiCallSpan = tracer.nextSpan().name(message.getDescriptorForType().getFullName()).start();
        }

        try (Tracer.SpanInScope scope = tracer.withSpan(apiCallSpan)) {
          io.micrometer.tracing.TraceContext tc = apiCallSpan.context();

          Trace.Builder tcBuilder = Trace.newBuilder()
              .setTraceId(tc.traceId())
              .setSpanId(tc.spanId());
          if (tc.parentId() != null) {
            tcBuilder.setParentId(tc.parentId());
          }

          metadata = metadata.toBuilder().setTrace(tcBuilder).build();

          CompletableFuture<R> cf = super.executeAsync(serviceName, methodHandler, message, metadata);
          cf.whenComplete((r, t) -> {
            if (t != null) {
              apiCallSpan.error(t);
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
