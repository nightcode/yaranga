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

  boolean autoRead();

  BootstrapFactory bootstrapFactory();

  int maxBodyLengthBytes();

  int nThreads();

  <P> PacketReader<P> packetReader();

  <P> PacketWriter<P> packetWriter();

  Proxy proxy();

  boolean soKeepAlive();

  boolean soReuseAddress();

  SslContext sslContext();

  boolean tcpNoDelay();
}
