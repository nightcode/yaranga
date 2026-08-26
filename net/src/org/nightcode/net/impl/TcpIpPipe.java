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

import java.net.InetSocketAddress;
import java.util.Deque;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.nightcode.common.pool.Session;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.net.AbstractPipe;
import org.nightcode.net.CompletablePacketContext;
import org.nightcode.net.PacketRxHandler;
import org.nightcode.net.PacketTxHandler;
import org.nightcode.net.PipeContext;

/**
 * TcpIpPipe.
 *
 * @param <Q> the request packet
 * @param <R> the response packet
 */
public class TcpIpPipe<Q, R> extends AbstractPipe<InetSocketAddress, Q, R> {

  protected TcpIpPipe(PipeContext<InetSocketAddress, ? extends Session<InetSocketAddress>> context) {
    super(context);
  }

  protected TcpIpPipe(PipeContext<InetSocketAddress, ? extends Session<InetSocketAddress>> context,
                      Deque<CompletablePacketContext<Q>> queue) {
    super(context, queue);
  }

  @Override protected boolean initPipeline(Channel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    SslContext sslContext = context.bootstrapFactory().sslContext();
    if (sslContext != null) {
      InetSocketAddress address = endpoint.resolve();
      if (address.isUnresolved()) {
        errorsConnect.incrementAndGet();
        closeChannel(ch);
        if (state.compareAndSet(State.CREATING, State.IDLE)) {
          scheduleReconnect(new IllegalStateException("endpoint not resolved [" + endpoint + "]"));
        }
        return false;
      }
      pipeline.addLast("ssl", sslContext.newHandler(ch.alloc(), address.getAddress().getHostAddress(), address.getPort()));
    }
    if (loggingEnabled) {
      pipeline.addLast("logger", new LoggingHandler(context.sessionName(), logLevel));
    }
    pipeline.addLast("out", new PacketTxHandler<>());
    pipeline.addLast("in", new PacketRxHandler<>(context.packetReader(), this::consume));

    return true;
  }

  @Override protected Bootstrap remoteAddress(Bootstrap bootstrap, Endpoint<InetSocketAddress> en) {
    return bootstrap.remoteAddress(en.resolve());
  }
}
