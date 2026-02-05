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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import org.nightcode.api.http.HttpApiGw;
import org.nightcode.api.http.HttpApiPipeFactory;
import org.nightcode.api.http.HttpApiPoolBuilder;
import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.SessionPoolBuilder;
import org.nightcode.common.pool.metadata.UriEndpoint;
import org.nightcode.common.pool.retry.RetryPolicy;
import org.nightcode.net.impl.ProtobufPacketReader;
import org.nightcode.net.impl.ProtobufPacketWriter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link HttpApiGw}.
 */
public class HttpApiGwTest {

  @ApiDefinition(name = "org.nightcode.api:TestApi", generation = 1)
  private interface TestApi {
    Response execute(Request request);

    CompletableFuture<Response> executeAsync(Request request);
  }

  @Test public void testHttpApiGateway() throws Exception {
    int remotePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      remotePort = socket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    InetSocketAddress address = new InetSocketAddress("127.0.0.1", remotePort);

    ApiHandler ah = new ApiHandler() {
      @Override public Class<? extends Message> findClass(String typeUrl) {
        return Request.class;
      }

      @Override public <Q extends Message, R extends Message> MethodHandler<Q, R> handlerFor(String typeUrl) {
        // noinspection unchecked
        return request -> (CompletableFuture<R>) CompletableFuture.completedFuture(request);
      }

      @Override public String name() {
        return "TEST";
      }
    };

    try (HttpApiGw gateway = HttpApiGw.build(ApiGwBuilder.builder()
        .name("test-gateway")
        .address(address)
        .serviceNameProvider(Request::getService))) {

      gateway.addApiHandler(ah.name(), ah);
      gateway.startAsync().get();

      SessionPoolBuilder<URI, ApiPipe<URI>> builder = SessionPoolBuilder.instance("test-pool");
      builder
          .addEndpoint(new UriEndpoint(new URI("http://" + address.getHostName() + ':' + address.getPort())))
          .sessionFactory(HttpApiPipeFactory.builder()
              .packetReader(new ProtobufPacketReader<>(Response.getDefaultInstance()))
              .packetWriter(new ProtobufPacketWriter<>())
              .build()
          );

      try (SessionPool<URI, ApiPipe<URI>> pool = builder.build()) {
        pool.init().get(500, TimeUnit.MILLISECONDS);

        Any payload = Any.pack(Request.newBuilder().setService("PAYLOAD").setPayload(Any.getDefaultInstance()).build());

        Request request = Request.newBuilder()
            .setService("TEST")
            .setPayload(payload)
            .build();

        ApiContext<URI>     context = new ApiContextImpl<>("TEST", null, pool, Metadata.getDefaultInstance(), 100, 3);
        ApiCallHandler<URI> rh      = new ApiCallHandler<>(request, RetryPolicy.def(), context);
        Response            r       = rh.sendReceiveAsync().get(5, TimeUnit.SECONDS);

        Assert.assertEquals(payload, r.getContent());
      }
    }
  }

  @Test public void testHttpExecute() throws Exception {
    int remotePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      remotePort = socket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    InetSocketAddress address = new InetSocketAddress("127.0.0.1", remotePort);

    ApiHandler ah = new ApiHandler() {
      @Override public Class<? extends Message> findClass(String typeUrl) {
        return Request.class;
      }

      @Override public <Q extends Message, R extends Message> MethodHandler<Q, R> handlerFor(String typeUrl) {
        return request -> {
          Assert.assertEquals(Request.getDefaultInstance().getDescriptorForType(), request.getDescriptorForType());
          Assert.assertEquals("test-service-request", ((Request) request).getService());
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
        .address(address);

    HttpApiPoolBuilder poolBuilder = HttpApiPoolBuilder.builder()
        .name(getClass().getSimpleName())
        .uri(new URI("http://" + address.getHostName() + ':' + address.getPort()));

    try (HttpApiGw gateway = HttpApiGw.build(apiGatewayBuilder)) {
      gateway.addApiHandler(ah.name(), ah);
      gateway.startAsync().get();

      try (SessionPool<URI, ApiPipe<URI>> pool = poolBuilder.build()) {
        pool.init().get();

        TestApi api = ApiFactory.def(pool).createApi(TestApi.class);
        Request request = Request.newBuilder()
            .setService("test-service-request")
            .build();

        Response response = api.execute(request);
        Assert.assertEquals("test-service-response", response.getService());
      }
    }
  }

  @Test public void testHttpMutualSslContext() throws Exception {
    int remotePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      remotePort = socket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    InetSocketAddress address = new InetSocketAddress("127.0.0.1", remotePort);

    ApiHandler ah = new ApiHandler() {
      @Override public Class<? extends Message> findClass(String typeUrl) {
        return Request.class;
      }

      @Override public <Q extends Message, R extends Message> MethodHandler<Q, R> handlerFor(String typeUrl) {
        return request -> {
          Assert.assertEquals(Request.getDefaultInstance().getDescriptorForType(), request.getDescriptorForType());
          Assert.assertEquals("test-service-request", ((Request) request).getService());
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
        .sslContext(serverContext(true));

    HttpApiPoolBuilder apiConnectionPoolBuilder = HttpApiPoolBuilder.builder()
        .name(getClass().getSimpleName())
        .uri(new URI("http://" + address.getHostName() + ':' + address.getPort()))
        .sslContext(clientContext(true));

    try (HttpApiGw gateway = HttpApiGw.build(apiGatewayBuilder)) {
      gateway.addApiHandler(ah.name(), ah);
      gateway.startAsync().get();

      try (SessionPool<URI, ApiPipe<URI>> pool = apiConnectionPoolBuilder.build()) {
        pool.init().get();

        TestApi api = ApiFactory.def(pool).createApi(TestApi.class);
        Request request = Request.newBuilder()
            .setService("test-service-request")
            .build();

        Response response = api.executeAsync(request).get();
        Assert.assertEquals("test-service-response", response.getService());
      }
    }
  }

  @Test public void testHttpServerSslContext() throws Exception {
    int remotePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      remotePort = socket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    InetSocketAddress address = new InetSocketAddress("127.0.0.1", remotePort);

    ApiHandler ah = new ApiHandler() {
      @Override public Class<? extends Message> findClass(String typeUrl) {
        return Request.class;
      }

      @Override public <Q extends Message, R extends Message> MethodHandler<Q, R> handlerFor(String typeUrl) {
        return request -> {
          Assert.assertEquals(Request.getDefaultInstance().getDescriptorForType(), request.getDescriptorForType());
          Assert.assertEquals("test-service-request", ((Request) request).getService());
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
        .sslContext(serverContext(false));

    HttpApiPoolBuilder poolBuilder = HttpApiPoolBuilder.builder()
        .name(getClass().getSimpleName())
        .uri(new URI("http://" + address.getHostName() + ':' + address.getPort()))
        .sslContext(clientContext(false));

    try (HttpApiGw gateway = HttpApiGw.build(apiGatewayBuilder)) {
      gateway.addApiHandler(ah.name(), ah);
      gateway.startAsync().get();

      try (SessionPool<URI, ApiPipe<URI>> pool = poolBuilder.build()) {
        pool.init().get();

        TestApi api = ApiFactory.def(pool).createApi(TestApi.class);
        Request request = Request.newBuilder()
            .setService("test-service-request")
            .build();

        Response response = api.executeAsync(request).get();
        Assert.assertEquals("test-service-response", response.getService());
      }
    }
  }

  private SslContext clientContext(boolean withKeyStore) {
    char[]            storePassword = "changeme".toCharArray();
    SslContextBuilder builder       = SslContextBuilder.builder().trustManager(storePassword, "test-resources/rsa/client/truststore.p12");
    if (withKeyStore) {
      builder.keyManager(storePassword, "test-resources/rsa/client/keystore.p12");
    }
    return builder.buildForClient();
  }

  private SslContext serverContext(boolean withTrustStore) {
    char[]            storePassword = "changeme".toCharArray();
    SslContextBuilder builder       = SslContextBuilder.builder().keyManager(storePassword, "test-resources/rsa/server/keystore.p12");
    if (withTrustStore) {
      builder.clientAuth(ClientAuth.REQUIRE).trustManager(storePassword, "test-resources/rsa/server/truststore.p12");
    }
    return builder.buildForServer();
  }
}
