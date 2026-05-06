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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.api.tcp.TcpIpApiGw;
import org.nightcode.api.tcp.TcpIpApiPoolBuilder;
import org.nightcode.common.pool.SessionPool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link ApiInterceptor}.
 */
public class ApiInterceptorTest {

  @ApiDefinition(name = "org.nightcode.api:TestApi", generation = 1)
  private interface TestApi {
    Response execute(Request request);

    CompletableFuture<Response> executeAsync(Request request);
  }

  @Test public void testExecute() throws Exception {
    int remotePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      remotePort = socket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    InetSocketAddress address = new InetSocketAddress("127.0.0.1", remotePort);

    int requestGeneration = ThreadLocalRandom.current().nextInt();

    ApiHandler ah = new ApiHandler() {
      @Override public Class<? extends Message> findClass(String typeUrl) {
        return Request.class;
      }

      @Override public <Q extends Message, R extends Message> MethodHandler<Q, R> handlerFor(String typeUrl) {
        return request -> {
          assertEquals(Request.getDefaultInstance().getDescriptorForType(), request.getDescriptorForType());
          assertEquals("test-service-request", ((Request) request).getService());
          // noinspection unchecked
          return CompletableFuture.completedFuture((R) Response.newBuilder().setService("test-service-response").build());
        };
      }

      @Override public String name() {
        return TestApi.class.getAnnotation(ApiDefinition.class).name();
      }
    };

    ApiGwBuilder apiGatewayBuilder = ApiGwBuilder.builder()
        .name("test-gateway")
        .address(address)
        .interceptor(new ApiGwInterceptor() {
          @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> intercept(ApiGwContext context) {
            return new SimpleApiGwCall<>(context.newApiCall()) {
              @Override public CompletableFuture<R> executeAsync(String serviceName, MethodHandler<Q, R> methodHandler, Q message, Metadata metadata) {
                assertEquals(requestGeneration, metadata.getGeneration());
                return super.executeAsync(serviceName, methodHandler, message, metadata);
              }
            };
          }
        });

    TcpIpApiPoolBuilder poolBuilder = TcpIpApiPoolBuilder.builder()
        .name(getClass().getSimpleName())
        .address(address);

    try (TcpIpApiGw gateway = TcpIpApiGw.build(apiGatewayBuilder)) {
      gateway.addApiHandler(ah.name(), ah);
      gateway.startAsync().get();

      try (SessionPool<InetSocketAddress, ApiPipe<InetSocketAddress>> pool = poolBuilder.build()) {
        pool.init().get();

        TestApi api = ApiFactory.def(pool).createApi(ApiConfig.builder(TestApi.class).interceptor(new ApiInterceptor() {
          @Override public <A, Q extends Message, R extends Message> ApiCall<Q, R> intercept(ApiContext<A> context, Class<Q> requestClass, Class<R> responseClass) {
            return new SimpleApiCall<Q, R>(context.newApiCall(requestClass, responseClass)) {
              @Override public CompletableFuture<R> executeAsync(Q message, Metadata metadata) {
                metadata = metadata.toBuilder().setGeneration(requestGeneration).build();
                return super.executeAsync(message, metadata);
              }
            };
          }
        }).build());
        Request request = Request.newBuilder()
            .setService("test-service-request")
            .build();

        Response response = api.execute(request);
        assertEquals("test-service-response", response.getService());
      }
    }
  }
}
