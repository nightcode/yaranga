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

import io.prometheus.metrics.core.metrics.Summary;
import org.nightcode.api.ApiGwCall;
import org.nightcode.api.ApiGwContext;
import org.nightcode.api.ApiGwInterceptor;
import org.nightcode.api.MethodHandler;
import org.nightcode.api.SimpleApiGwCall;
import org.nightcode.api.message.Metadata;

/**
 * Metrics API gateway interceptor.
 */
public class MetricsApiGwInterceptor implements ApiGwInterceptor {

  public enum Transport {
    HTTP, TCP
  }

  private static final Summary REQUEST_TIMER = Summary.builder()
      .name("api_gateway_requests")
      .help("api gateway method invocation")
      .labelNames("transport", "service", "generation", "method")
      .register();

  private final Transport transport;

  public MetricsApiGwInterceptor(Transport transport) {
    this.transport = transport;
  }

  @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> intercept(ApiGwContext context) {
    return new SimpleApiGwCall<>(context.newApiCall()) {
      @Override public CompletableFuture<R> executeAsync(String serviceName, MethodHandler<Q, R> methodHandler, Q message,
                                                         Metadata metadata) {
        var timer = REQUEST_TIMER.labelValues(transport.name(), serviceName, Integer.toString(metadata.getGeneration())
            , message.getDescriptorForType().getName()).startTimer();
        var cf = super.executeAsync(serviceName, methodHandler, message, metadata);
        cf.whenComplete((r, t) -> timer.observeDuration());
        return cf;
      }
    };
  }

  @Override public String toString() {
    return getClass().getSimpleName();
  }
}
