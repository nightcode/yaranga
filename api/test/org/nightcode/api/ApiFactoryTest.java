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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;

import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.SessionPoolBuilder;
import org.nightcode.common.pool.metadata.InetSocketAddressEndpoint;
import org.nightcode.net.BootstrapFactory;
import org.nightcode.net.impl.ProtobufPacketReader;
import org.nightcode.net.impl.ProtobufPacketWriter;
import org.nightcode.net.impl.TcpIpPipeFactory;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ApiFactory}.
 */
public class ApiFactoryTest {

  @ApiDefinition(name = "TEST", generation = 1)
  interface ApiTest {
    Response execute(Request request);

    CompletableFuture<Response> executeAsync(Request request);
  }

  @Test public void testCreate() {
    int remotePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      remotePort = socket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    InetSocketAddress address = new InetSocketAddress("127.0.0.1", remotePort);

    SessionPoolBuilder<InetSocketAddress, ApiPipe<InetSocketAddress>> builder = SessionPoolBuilder.instance("test-pool");
    builder
        .addEndpoint(new InetSocketAddressEndpoint(address))
        .sessionFactory(TcpIpPipeFactory.builder()
            .bootstrapFactory(BootstrapFactory.tcpIpFactory())
            .packetReader(new ProtobufPacketReader<>(Response.getDefaultInstance()))
            .packetWriter(new ProtobufPacketWriter<>())
            .build()
        );

    try (SessionPool<InetSocketAddress, ApiPipe<InetSocketAddress>> pool = builder.build()) {
      pool.init();
      ApiFactory.def(pool).createApi(ApiTest.class);
    }
  }
}
