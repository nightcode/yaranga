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

import io.netty.bootstrap.ServerBootstrap;

/**
 * Server bootstrap factory.
 */
public interface BootstrapServerFactory {

  static BootstrapServerFactory tcpIpServerFactory() {
    return new TcpIpServerFactory();
  }

  static BootstrapServerFactory unixSocketServerFactory() {
    return new UnixSocketServerFactory();
  }

  ServerBootstrap create(String name, int nThreads);
}
