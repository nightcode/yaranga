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

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.nightcode.api.message.Metadata;
import org.nightcode.common.logging.Log;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.props.Properties;

/**
 * Default API factory.
 *
 * @param <A> the address
 */
class ApiFactoryImpl<A> implements ApiFactory {

  private record InterceptApiContext<A>(ApiContext<A> delegate, ApiInterceptor interceptor) implements ApiContext<A> {

    @Override public SessionPool<A, ApiPipe<A>> connectionPool() {
      return delegate.connectionPool();
    }

    @Override public long executeTimeoutMs() {
      return delegate.executeTimeoutMs();
    }

    @Override public int maxAttempts() {
      return delegate.maxAttempts();
    }

    @Override public Metadata metadata() {
      return delegate.metadata();
    }

    @Override public <Q extends Message, R extends Message> ApiCall<Q, R> newApiCall(Class<Q> requestClass, Class<R> responseClass) {
      return interceptor.intercept(delegate, requestClass, responseClass);
    }

    @Override public <M extends Message> Any packMessage(M message) {
      return delegate.packMessage(message);
    }

    @Override public String serviceName() {
      return delegate.serviceName();
    }
  }

  protected static final int MAX_ATTEMPTS = Properties.instance().getInt("org.nightcode.api.MaxAttempts", 3);

  public static <T> ApiConfig config(Class<T> apiInterface) {
    return ApiConfig.builder(apiInterface).build();
  }

  private static <A> ApiContext<A> intercept(ApiContext<A> context, List<ApiInterceptor> interceptors) {
    for (ApiInterceptor interceptor : interceptors) {
      context = new InterceptApiContext<>(context, interceptor);
    }
    return context;
  }

  private final SessionPool<A, ApiPipe<A>> connectionPool;

  ApiFactoryImpl(SessionPool<A, ApiPipe<A>> connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override public <T> T createApi(Class<T> apiInterface) {
    if (!apiInterface.isAnnotationPresent(ApiDefinition.class)) {
      throw new IllegalArgumentException("interface " + apiInterface + " must be annotated with @" + ApiDefinition.class + " annotation");
    }
    ApiDefinition definition = apiInterface.getAnnotation(ApiDefinition.class);
    return createApi(apiInterface, Collections.emptyList(), definition.name(), definition.generation(), definition.executeTimeoutMs());
  }

  @Override public <T> T createApi(ApiConfig cfg) {
    // noinspection unchecked
    return createApi((Class<T>) cfg.apiInterface(), cfg.interceptors(), cfg.serviceName(), cfg.generation(), cfg.executeTimeoutMs());
  }

  private <T> T createApi(Class<T> apiInterface, List<ApiInterceptor> interceptors, String serviceName, int generation,
                          long executeTimeoutMs) {
    Objects.requireNonNull(apiInterface, "API interface");
    Objects.requireNonNull(serviceName, "service name");
    MessagePackager packager = new MessagePackager() {
      @Override public <M extends Message> Any pack(M message) {
        return Any.pack(message, serviceName);
      }
    };
    Metadata metadata = Metadata.newBuilder().setGeneration(generation).build();

    ApiContext<A> context = createContext(serviceName, packager, metadata, executeTimeoutMs);

    Log.info().log(getClass(), "[{}] api service interceptors: {}", serviceName, interceptors);
    context = intercept(context, interceptors);

    InvocationHandler handler = new ApiInvocationHandler<>(apiInterface, context);
    // noinspection unchecked
    return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{apiInterface}, handler);
  }

  private ApiContext<A> createContext(String serviceName, MessagePackager packager, Metadata metadata, long executeTimeoutMs) {
    return new ApiContextImpl<>(serviceName, packager, connectionPool, metadata, executeTimeoutMs, MAX_ATTEMPTS);
  }
}
