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
import org.nightcode.api.ApiCall;
import org.nightcode.api.ApiContext;
import org.nightcode.api.ApiInterceptor;
import org.nightcode.api.SimpleApiCall;
import org.nightcode.api.message.Metadata;

/**
 * Metrics API interceptor.
 */
public enum MetricsApiInterceptor implements ApiInterceptor {
  INSTANCE;

  private static final Summary TIMER = Summary.builder()
      .name("api_requests")
      .help("api method invocation")
      .labelNames("service", "generation", "method")
      .register();

  @Override public <A, Q extends Message, R extends Message> ApiCall<Q, R> intercept(ApiContext<A> clientApiContext,
                                                                                     Class<Q> requestClass,
                                                                                     Class<R> responseClass) {
    return new SimpleApiCall<>(clientApiContext.newApiCall(requestClass, responseClass)) {
      @Override public CompletableFuture<R> executeAsync(Q message, Metadata metadata) {
        var timer = TIMER.labelValues(clientApiContext.serviceName(), Integer.toString(metadata.getGeneration())
            , message.getDescriptorForType().getName()).startTimer();
        var cf = super.executeAsync(message, metadata);
        cf.whenComplete((r, t) -> timer.observeDuration());
        return cf;
      }
    };
  }

  @Override public String toString() {
    return getClass().getSimpleName();
  }
}
