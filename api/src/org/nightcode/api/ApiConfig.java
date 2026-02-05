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

package org.nightcode.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API configuration.
 */
public final class ApiConfig {

  public static final class Builder {
    private final Class<?> apiInterface;

    private String serviceName;

    private int  generation       = 1;
    private long executeTimeoutMs = 5_000L;

    private List<ApiInterceptor> interceptors = Collections.emptyList();

    private Builder(Class<?> apiInterface) {
      this.apiInterface = apiInterface;
      if (apiInterface.isAnnotationPresent(ApiDefinition.class)) {
        ApiDefinition definition = apiInterface.getAnnotation(ApiDefinition.class);
        serviceName      = definition.name();
        generation       = definition.generation();
        executeTimeoutMs = definition.executeTimeoutMs();
      }
    }

    public ApiConfig build() {
      return new ApiConfig(this);
    }

    public Builder interceptor(ApiInterceptor... val) {
      interceptors = Arrays.asList(val);
      return this;
    }

    public Builder executeTimeout(long val, TimeUnit unit) {
      executeTimeoutMs = unit.toMillis(val);
      return this;
    }

    public Builder generation(int val) {
      generation = val;
      return this;
    }

    public Builder serviceName(String val) {
      serviceName = val;
      return this;
    }
  }

  public static Builder builder(Class<?> apiInterface) {
    return new Builder(apiInterface);
  }

  private final Class<?>             apiInterface;
  private final String               serviceName;
  private final int                  generation;
  private final long                 executeTimeoutMs;
  private final List<ApiInterceptor> interceptors;

  private ApiConfig(Builder builder) {
    apiInterface     = builder.apiInterface;
    serviceName      = builder.serviceName;
    generation       = builder.generation;
    executeTimeoutMs = builder.executeTimeoutMs;
    interceptors     = Collections.unmodifiableList(builder.interceptors);
  }

  public Class<?> apiInterface() {
    return apiInterface;
  }

  public List<ApiInterceptor> interceptors() {
    return interceptors;
  }

  public long executeTimeoutMs() {
    return executeTimeoutMs;
  }

  public int generation() {
    return generation;
  }

  public String serviceName() {
    return serviceName;
  }
}
