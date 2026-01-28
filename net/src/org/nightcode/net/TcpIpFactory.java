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

import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.nightcode.common.pool.Session;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.common.logging.Log;

/**
 * TcpIp implementation of BootstrapFactory.
 */
public class TcpIpFactory implements BootstrapFactory {

  @Override public Bootstrap create(PipeContext<?, ? extends Session<?>> context) {
    Supplier<IoHandlerFactory> factorySupplier;
    Class<? extends Channel>   channelClass;

    if (Epoll.isAvailable()) {
      factorySupplier = EpollIoHandler::newFactory;
      channelClass    = EpollSocketChannel.class;
      Log.info().log(getClass(), "initialize netty EPOLL transport");
    } else if (KQueue.isAvailable()) {
      factorySupplier = KQueueIoHandler::newFactory;
      channelClass = KQueueSocketChannel.class;
      Log.info().log(getClass(), "initialize netty KQUEUE transport");
    } else {
      factorySupplier = NioIoHandler::newFactory;
      channelClass    = NioSocketChannel.class;
    }

    Bootstrap     b  = new Bootstrap();
    ThreadFactory tf = ExecutorUtils.namedThreadFactory(context.sessionName() + "-" + channelClass.getSimpleName());

    b.group(new MultiThreadIoEventLoopGroup(context.nThreads(), tf, factorySupplier.get())).channel(channelClass);

    b.option(ChannelOption.SO_KEEPALIVE, context.soKeepAlive());
    b.option(ChannelOption.SO_REUSEADDR, context.soReuseAddress());
    b.option(ChannelOption.TCP_NODELAY, context.tcpNoDelay());
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) context.createTimeoutMs());

    b.option(ChannelOption.AUTO_READ, context.autoRead());
    b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

    return b;
  }
}
