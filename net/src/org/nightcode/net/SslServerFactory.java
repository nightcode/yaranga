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

import java.net.SocketAddress;
import java.util.Objects;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.handler.ssl.SslContext;

public class SslServerFactory<A extends SocketAddress> implements BootstrapServerFactory<A> {

  private final BootstrapServerFactory<A> target;
  private final SslContext                sslContext;

  public SslServerFactory(BootstrapServerFactory<A> target, SslContext sslContext) {
    this.target     = Objects.requireNonNull(target, "delegate");
    this.sslContext = Objects.requireNonNull(sslContext, "sslContext");
  }

  @Override public ServerBootstrap create(String name, int nThreads) {
    return target.create(name, nThreads);
  }

  @Override public A localAddress() {
    return target.localAddress();
  }

  @Override public SslContext sslContext() {
    return sslContext;
  }
}
