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
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.Nullable;
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

  default BootstrapFactory withSsl(SslContextConfig config) {
    if (config.useSsl()) {
      char[] truststorePasswd = config.truststorePassword().toCharArray();
      SslContextBuilder builder = SslContextBuilder.builder()
          .trustManager(truststorePasswd, config.truststorePath());
      if (config.mutualSsl()) {
        char[] keystorePasswd = config.keystorePassword().toCharArray();
        builder.keyManager(keystorePasswd, config.keystorePath());
      }
      return withSsl(builder.buildForClient());
    }
    return this;
  }

  default BootstrapFactory withSsl(@Nullable SslContext sslContext) {
    if (sslContext == null) {
      return this;
    }
    return new SslFactory(this, sslContext);
  }

  Bootstrap create(PipeContext<?, ? extends Session<?>> context);

  default @Nullable SslContext sslContext() {
    return null;
  }
}
