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

import io.netty.bootstrap.Bootstrap;
import org.nightcode.common.pool.Session;

/**
 * Bootstrap factory.
 */
public interface BootstrapFactory {

  static BootstrapFactory tcpIpFactory() {
    return new TcpIpFactory();
  }

  static BootstrapFactory unixSocketFactory() {
    return new UnixSocketFactory();
  }

  Bootstrap create(PipeContext<?, ? extends Session<?>> context);
}
