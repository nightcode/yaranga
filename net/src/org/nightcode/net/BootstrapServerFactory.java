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

package org.nightcode.net;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.unix.DomainSocketAddress;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.pool.metadata.InetSocketAddressEndpoint;

/**
 * Server bootstrap factory.
 *
 * @param <A> address
 */
public interface BootstrapServerFactory<A extends SocketAddress> {

  static BootstrapServerFactory<InetSocketAddress> tcpIpServerFactory(String address) {
    Endpoint<InetSocketAddress> endpoint = new InetSocketAddressEndpoint(address);
    return new TcpIpServerFactory(endpoint.resolve());
  }

  static BootstrapServerFactory<InetSocketAddress> tcpIpServerFactory(InetSocketAddress address) {
    return new TcpIpServerFactory(address);
  }

  static BootstrapServerFactory<DomainSocketAddress> unixSocketServerFactory(DomainSocketAddress address) {
    return new UnixSocketServerFactory(address);
  }

  ServerBootstrap create(String name, int nThreads);

  A localAddress();
}
