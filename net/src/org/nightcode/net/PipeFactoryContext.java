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

import java.net.Proxy;

import io.netty.handler.ssl.SslContext;

/**
 * Connection factory context.
 */
public interface PipeFactoryContext {

  BootstrapFactory bootstrapFactory();

  int nThreads();

  <P> PacketReader<P> packetReader();

  <P> PacketWriter<P> packetWriter();

  SslContext sslContext();

  default boolean autoRead() {
    return true;
  }

  default int maxBodyLengthBytes() {
    return 1024 * 1024;
  }

  default Proxy proxy() {
    return Proxy.NO_PROXY;
  }

  default boolean soKeepAlive() {
    return true;
  }

  default boolean soReuseAddress() {
    return true;
  }

  default boolean tcpNoDelay() {
    return true;
  }
}
