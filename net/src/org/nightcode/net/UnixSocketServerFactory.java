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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;
import org.nightcode.common.util.ExecutorUtils;

/**
 * Unix socket implementation of BootstrapServerFactory.
 */
class UnixSocketServerFactory implements BootstrapServerFactory {

  @Override public ServerBootstrap create(String name, int nThreads) {
    Supplier<IoHandlerFactory>     factorySupplier;
    Class<? extends ServerChannel> channelClass;

    if (Epoll.isAvailable()) {
      factorySupplier = EpollIoHandler::newFactory;
      channelClass    = EpollServerDomainSocketChannel.class;
      Log.info().log(getClass(), "initialize netty EPOLL transport");
    } else if (KQueue.isAvailable()) {
      factorySupplier = KQueueIoHandler::newFactory;
      channelClass    = KQueueServerDomainSocketChannel.class;
      Log.info().log(getClass(), "initialize netty KQUEUE transport");
    } else {
      throw new IllegalStateException("netty native transport (Epoll/KQueue) is required for Unix Domain Socket");
    }

    ServerBootstrap sb         = new ServerBootstrap();
    ThreadFactory   acceptorTf = ExecutorUtils.namedThreadFactory(name + "-" + channelClass.getSimpleName() + "-acceptor");
    ThreadFactory   workerTf   = ExecutorUtils.namedThreadFactory(name + "-" + channelClass.getSimpleName() + "-worker");

    sb.group(new SingleThreadIoEventLoop(null, acceptorTf, factorySupplier.get())
            , new MultiThreadIoEventLoopGroup(nThreads, workerTf, factorySupplier.get()))
        .channel(channelClass);

    String prefix = "org.nightcode.net.unix." + name + '.';

    sb.option(ChannelOption.SO_BACKLOG, Properties.instance().getInt(prefix + "backlog", 1024));

    sb.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

    return sb;
  }
}
