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
import java.net.Proxy;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import org.nightcode.net.CompletablePacketContext;
import org.nightcode.net.Pipe;
import org.nightcode.net.PipeContext;
import org.nightcode.net.ConnectionTimeoutException;
import org.nightcode.net.PacketFuture;
import org.nightcode.net.PacketRxHandler;
import org.nightcode.net.PacketTxHandler;
import org.nightcode.common.net.MessageQueue;
import org.nightcode.common.net.SimpleMessageQueue;
import org.nightcode.common.pool.AbstractSession;
import org.nightcode.common.pool.Session;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * todo.
 *
 * @param <Q> the request packet
 * @param <R> the response packet
 */
public class TcpIpPipe<Q, R> extends AbstractSession<InetSocketAddress>
    implements Pipe<InetSocketAddress, Q, R>, ChannelFutureListener {

  private static final AtomicLong PACKET_ID = new AtomicLong(1);

  private final PipeContext<InetSocketAddress, ? extends Session<InetSocketAddress>> context;

  private final boolean  loggingEnabled;
  private final LogLevel logLevel;

  private volatile Channel   channel;
  private volatile Future<?> reconnectFuture;

  private final ExecutorService           executor;
  private final ScheduledExecutorService  pipeExecutor;
  private final MultithreadEventLoopGroup workerGroup;

  private final Bootstrap bootstrap;

  private final Deque<CompletablePacketContext<Q>>        queue;
  private final MessageQueue<CompletablePacketContext<Q>> packetQueue;

  private final   AtomicLong requestsCount = new AtomicLong(0);
  protected final AtomicLong errorsConnect = new AtomicLong(0);
  protected final AtomicLong errorsTimeout = new AtomicLong(0);

  private final ConcurrentMap<Long, CompletableFuture<R>> cfs = new ConcurrentHashMap<>();

  protected TcpIpPipe(PipeContext<InetSocketAddress, ? extends Session<InetSocketAddress>> context) {
    this(context, new ConcurrentLinkedDeque<>());
  }

  protected TcpIpPipe(PipeContext<InetSocketAddress, ? extends Session<InetSocketAddress>> context,
                      Deque<CompletablePacketContext<Q>> queue) {
    super(context.endpoint());
    this.context = context;
    this.queue   = queue;

    loggingEnabled = Properties.instance().getBoolean(context.sessionName() + ".loggingEnabled", false);
    logLevel       = LogLevel.valueOf(Properties.instance().getString(context.sessionName() + ".logLevel", "DEBUG"));

    packetQueue = new SimpleMessageQueue<>(queue, this::sendPacket);

    executor     = ExecutorUtils.singleThreadExecutor(context.sessionName());
    pipeExecutor = ExecutorUtils.scheduledExecutorService(context.sessionName() + "Pipe", 1);

    bootstrap   = context.bootstrapFactory().create(context).remoteAddress(endpoint.resolve());
    workerGroup = (MultithreadEventLoopGroup) bootstrap.config().group();
  }

  public void activate() {
    createImpl();
  }

  public MultithreadEventLoopGroup eventLoopGroup() {
    return workerGroup;
  }

  public long errorsConnect() {
    return errorsConnect.get();
  }

  public long errorsTimeout() {
    return errorsTimeout.get();
  }

  public int queueSize() {
    return packetQueue.size();
  }

  public int queueState() {
    return packetQueue.state().state();
  }

  public long requestsCount() {
    return requestsCount.get();
  }

  @Override protected void destroyImpl() {
    ExecutorUtils.shutdown(executor);
    ExecutorUtils.shutdown(pipeExecutor);

    try {
      closeChannel(channel);
      channel = null;
    } catch (Exception ex) {
      Log.info().log(getClass(), ex);
    }
    workerGroup.shutdownGracefully();

    queue.clear();
    cfs.forEach((id, cf) -> cf.completeExceptionally(EOF));
    cfs.clear();

    Log.warn().log(getClass(), "pipe {} closed", this);
  }

  public void deactivate() {
    State currentState = state.get();
    if (State.DESTROYED == currentState || State.NEW == currentState) {
      return;
    }
    tryCancelScheduledFutures();
    currentState = state.updateAndGet(state -> state != State.DESTROYED ? State.IDLE : State.DESTROYED);
    if (State.IDLE == currentState) {
      fireStateEvent(State.IDLE);
      closeChannel(channel);
    }
  }

  @Override public void operationComplete(ChannelFuture future) {
    future.removeListener(this);
    closeChannel(future.channel());

    if (state.compareAndSet(State.ACTIVE, State.IDLE)) {
      fireStateEvent(State.IDLE);
      Throwable cause = future.cause();
      scheduleReconnect(cause);
    }
  }

  @Override protected void createImpl() {
    if (!state.compareAndSet(State.IDLE, State.CREATING)) {
      return;
    }

    Log.debug().log(getClass(), "connecting to " + endpoint);

    fireStateEvent(State.CREATING);

    tryCancelScheduledFutures();

    ChannelFuture connectFuture = bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (!Proxy.NO_PROXY.equals(context.proxy()) && Proxy.Type.SOCKS.equals(context.proxy().type())) {
          p.addLast("proxy-socks5", new Socks5ProxyHandler(context.proxy().address()));
        }
      }
    }).connect();
    connectFuture.addListener(this::connectCallback);
  }

  @Override public CompletableFuture<Void> sendAsync(Q packet) {
    requestsCount.incrementAndGet();
    CompletablePacketContext<Q> packetContext = packetContext(packet);

    packetQueue.put(packetContext);
    if (State.ACTIVE == state.get()) {
      executor.execute(packetQueue::flush);
    }

    packetContext.completion().whenComplete((v, t) -> {
      if (t != null) {
        packetQueue.remove(packetContext);
      }
    });
    return packetContext.completion();
  }

  @Override public CompletableFuture<R> sendReceiveAsync(Q packet) {
    requestsCount.incrementAndGet();
    CompletablePacketContext<Q> packetContext  = packetContext(packet);
    CompletableFuture<R>        responseFuture = new PacketFuture<>(packetContext.packetId(), (id, cause) -> cfs.remove(id));

    cfs.put(packetContext.packetId(), responseFuture);

    packetQueue.put(packetContext);
    if (State.ACTIVE == state.get()) {
      executor.execute(packetQueue::flush);
    }

    CompletableFuture<R> cf = packetContext.completion().handle((v, t) -> {
      if (t != null) {
        responseFuture.completeExceptionally(t);
      }
      return responseFuture;
    }).thenCompose(UnaryOperator.identity());

    cf.whenComplete((r, t) -> {
      if (t != null) {
        packetQueue.remove(packetContext);
      }
    });

    return cf;
  }

  @Override protected void tryCancelScheduledFutures() {
    Future<?> tmpFuture = reconnectFuture;
    if (tmpFuture != null) {
      tmpFuture.cancel(false);
      reconnectFuture = null;
    }
  }

  private void closeChannel(Channel channel) {
    packetQueue.tryInterrupt();
    if (channel != null) {
      channel.closeFuture().removeListener(this);
      channel.close();
      Log.info().log(getClass(), "channel {} closed", channel);
    }
  }

  private void connectCallback(Future<? super Void> future) {
    ChannelFuture channelFuture = (ChannelFuture) future;
    if (!connectCallback(channelFuture.channel(), channelFuture.cause())) {
      Log.info().log(getClass(), "pipe to {} was not established", endpoint);
    }
  }

  private boolean connectCallback(Channel channel, Throwable cause) {
    if (State.DESTROYED == state.get()) {
      closeChannel(channel);
      return false;
    }

    if (cause == null) {
      tryCancelScheduledFutures();

      ChannelPipeline pipeline   = channel.pipeline();
      SslContext      sslContext = context.sslContext();
      if (sslContext != null) {
        InetSocketAddress address = endpoint.resolve();
        pipeline.addLast("ssl", sslContext.newHandler(channel.alloc(), address.getAddress().getHostAddress(), address.getPort()));
      }
      if (loggingEnabled) {
        pipeline.addLast("logger", new LoggingHandler(context.sessionName(), logLevel));
      }
      pipeline.addLast("out", new PacketTxHandler<>());
      pipeline.addLast("in", new PacketRxHandler<>(context.packetReader(), this::consume));
      channel.closeFuture().addListener(this);

      this.channel = channel;
      packetQueue.resume();

      if (state.compareAndSet(State.CREATING, State.ACTIVE)) {
        Log.info().log(getClass(), "pipe {} established", channel);
        errorsConnect.set(0);
        fireStateEvent(State.ACTIVE);
        return true;
      }

      return false;
    }

    if (cause instanceof ConnectTimeoutException) {
      return connectionTimeout(channel);
    }

    errorsConnect.incrementAndGet();
    closeChannel(channel);

    if (state.compareAndSet(State.CREATING, State.IDLE)) {
      scheduleReconnect(cause);
    }
    return false;
  }

  private boolean connectionTimeout(Channel channel) {
    errorsTimeout.incrementAndGet();
    Log.debug().log(getClass(), "connection timeout event for {}, state: {}", endpoint, state.get());
    Future<?> future = reconnectFuture;
    if (future != null) {
      future.cancel(false);
      reconnectFuture = null;
    }

    if (State.DESTROYED == state.get()) {
      return true;
    }

    State currentState = state.updateAndGet(state -> state != State.ACTIVE ? State.TIMEOUT : State.ACTIVE);
    if (State.ACTIVE == currentState) {
      Log.debug().log(getClass(), () -> "no timeout actions were scheduled because pipe has been established");
      return false;
    }

    fireStateEvent(currentState);
    closeChannel(channel);

    if (state.compareAndSet(State.TIMEOUT, State.IDLE)) {
      fireStateEvent(State.IDLE);
      Throwable cause = new TimeoutException("timed out while connecting to " + endpoint);
      scheduleReconnect(cause);
    }
    return true;
  }

  private void consume(ChannelHandlerContext ctx, PacketHolder<R> holder) {
    CompletableFuture<R> cf = cfs.remove(holder.packetId());
    if (cf != null) {
      cf.complete(holder.packet());
    }
  }

  private void handleWriteAndFlush(CompletablePacketContext<Q> context, Channel channel, Throwable cause) {
    if (cause == null) {
      context.completion().complete(null);
      return;
    }
    if (cause instanceof ConnectionTimeoutException) {
      errorsTimeout.incrementAndGet();
    }
    context.completion().completeExceptionally(cause);
    closeChannel(channel);
    if (state.compareAndSet(State.ACTIVE, State.IDLE)) {
      fireStateEvent(State.IDLE);
      scheduleReconnect(cause);
    }
  }

  private CompletablePacketContext<Q> packetContext(Q packet) {
    long packetId    = PACKET_ID.incrementAndGet();
    long expiredOnNs = clock.nanoTime() + context.queueTimeoutNs();
    return new CompletablePacketContextImpl<>(context.packetWriter(), packetId, packet, expiredOnNs, clock);
  }

  private void scheduleReconnect(Throwable cause) {
    Log.debug().log(getClass(), cause, "reconnect to {} after {} ms.", endpoint, context.rebuildTimeoutMs());
    reconnectFuture = pipeExecutor.schedule(this::createImpl, context.rebuildTimeoutMs(), MILLISECONDS);
  }

  private void sendPacket(CompletablePacketContext<Q> context) {
    try {
      ChannelFuture channelFuture = channel.writeAndFlush(context);
      channelFuture.addListener(f -> handleWriteAndFlush(context, ((ChannelFuture) f).channel(), f.cause()));
    } catch (Exception ex) {
      Log.warn().log(getClass(), ex, "unable to send packet to {}", endpoint);
      context.completion().completeExceptionally(ex);
    }
  }
}
