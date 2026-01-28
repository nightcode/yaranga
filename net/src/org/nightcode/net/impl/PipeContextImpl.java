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

package org.nightcode.net.impl;

import java.net.Proxy;

import io.netty.handler.ssl.SslContext;
import org.nightcode.common.pool.Session;
import org.nightcode.common.pool.SessionContext;
import org.nightcode.common.pool.SessionContextImpl;
import org.nightcode.net.BootstrapFactory;
import org.nightcode.net.PacketReader;
import org.nightcode.net.PacketWriter;
import org.nightcode.net.PipeContext;
import org.nightcode.net.PipeFactoryContext;

/**
 * Default PipeContext implementation.
 *
 * @param <A> the pipe address
 * @param <S> the pipe
 */
public class PipeContextImpl<A, S extends Session<A>> extends SessionContextImpl<A, S> implements PipeContext<A, S> {

  private final PipeFactoryContext factoryContext;

  public PipeContextImpl(SessionContext<A, S> context, PipeFactoryContext factoryContext) {
    super(context.sessionName(), context.endpoint(), context.pool());
    this.factoryContext = factoryContext;
  }

  @Override public boolean autoRead() {
    return factoryContext.autoRead();
  }

  @Override public BootstrapFactory bootstrapFactory() {
    return factoryContext.bootstrapFactory();
  }

  @Override public int maxBodyLengthBytes() {
    return factoryContext.maxBodyLengthBytes();
  }

  @Override public int nThreads() {
    return factoryContext.nThreads();
  }

  @Override public <M> PacketReader<M> packetReader() {
    return factoryContext.packetReader();
  }

  @Override public <M> PacketWriter<M> packetWriter() {
    return factoryContext.packetWriter();
  }

  @Override public Proxy proxy() {
    return factoryContext.proxy();
  }

  @Override public boolean soKeepAlive() {
    return factoryContext.soKeepAlive();
  }

  @Override public boolean soReuseAddress() {
    return factoryContext.soReuseAddress();
  }

  @Override public SslContext sslContext() {
    return factoryContext.sslContext();
  }

  @Override public boolean tcpNoDelay() {
    return factoryContext.tcpNoDelay();
  }
}
