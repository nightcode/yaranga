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

import com.google.protobuf.Message;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.common.logging.Log;
import org.nightcode.common.util.ReflectUtils;

/**
 * API invocation handler.
 *
 * @param <T>
 */
public class ApiInvocationHandler<T> implements InvocationHandler {

  private static class ApiAsyncMethodInvoker<A, Q extends Message, R extends Message> implements MethodInvoker {
    private final ApiContext<A> context;
    private final Class<Q>      requestClass;
    private final Class<R>      responseClass;

    ApiAsyncMethodInvoker(ApiContext<A> context, Class<Q> requestClass, Class<R> responseClass) {
      this.context       = context;
      this.requestClass  = requestClass;
      this.responseClass = responseClass;
    }

    @Override public Object invoke(Object[] args) {
      // noinspection unchecked
      Q             request = (Q) args[0];
      ApiCall<Q, R> apiCall = context.newApiCall(requestClass, responseClass);
      return apiCall.executeAsync(request, context.metadata());
    }
  }

  private static class ApiMethodInvoker<A, Q extends Message, R extends Message> implements MethodInvoker {
    private final MethodInvoker delegate;

    ApiMethodInvoker(ApiContext<A> context, Class<Q> requestClass, Class<R> responseClass) {
      delegate = new ApiAsyncMethodInvoker<>(context, requestClass, responseClass);
    }

    @Override public Object invoke(Object[] args) throws ApiException {
      try {
        // noinspection unchecked
        return ((CompletableFuture<R>) delegate.invoke(args)).get();
      } catch (ExecutionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ApiException) {
          throw (ApiException) cause;
        }
        throw new ApiException(500, "unable to execute request", ex);
      } catch (Exception ex) {
        throw new ApiException(500, "unable to execute request", ex);
      }
    }
  }

  private static <A> MethodInvoker createMethodInvoker(Method method, ApiContext<A> context) {
    Type[] parameters = method.getParameterTypes();
    if (parameters.length != 1) {
      throw new IllegalStateException("method '" + method + "' must have only one parameter");
    }
    Type request  = ReflectUtils.resolveType(parameters[0]);
    Type response = ReflectUtils.resolveType(method.getGenericReturnType());

    Class<?> requestClass = (Class<?>) request;
    if (!Message.class.isAssignableFrom(requestClass)) {
      throw new IllegalArgumentException("method '" + method + "', argument should be instance of " + Message.class.getName() + " class");
    }

    if (response instanceof ParameterizedType parameterized && parameterized.getRawType().equals(CompletableFuture.class)) {
      Class<?> responseClass = (Class<?>) parameterized.getActualTypeArguments()[0];
      if (!Message.class.isAssignableFrom(responseClass)) {
        throw new IllegalStateException("CompletableFuture result type of method '" + method + "' should be instance of "
            + Message.class.getName() + " class");
      }
      // noinspection unchecked
      return new ApiAsyncMethodInvoker<>(context, (Class<Request>) requestClass, (Class<Response>) responseClass);
    }

    Class<?> responseClass = (Class<?>) response;
    if (Message.class.isAssignableFrom(responseClass)) {
      // noinspection unchecked
      return new ApiMethodInvoker<>(context, (Class<Request>) requestClass, (Class<Response>) responseClass);
    }

    throw new IllegalStateException("method '" + method + "' result type should be instance of " + Message.class.getName() + " class");
  }

  private final Class<T>                   apiInterface;
  private final Map<Method, MethodInvoker> map;

  public ApiInvocationHandler(Class<T> apiInterface, ApiContext<?> context) {
    this.apiInterface = apiInterface;

    Map<Method, MethodInvoker> tmpMap = new HashMap<>();
    for (Method method : apiInterface.getMethods()) {
      tmpMap.put(method, createMethodInvoker(method, context));
    }
    map = tmpMap;
  }

  @Override public Object invoke(Object proxy, Method method, Object[] args) {
    MethodInvoker methodInvoker = map.get(method);
    if (methodInvoker == null) {
      throw new IllegalStateException("no method " + method);
    }
    Log.debug().log(apiInterface, "invoking {}..", method.getName());
    return methodInvoker.invoke(args);
  }
}
