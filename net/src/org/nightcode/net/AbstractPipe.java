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
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
import io.netty.channel.StacklessClosedChannelException;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.jetbrains.annotations.Nullable;
import org.nightcode.common.annotations.Beta;
import org.nightcode.common.logging.Log;
import org.nightcode.common.net.MessageQueue;
import org.nightcode.common.net.SimpleMessageQueue;
import org.nightcode.common.pool.AbstractSession;
import org.nightcode.common.pool.Session;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.props.Properties;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.net.impl.CompletablePacketContextImpl;
import org.nightcode.net.impl.PacketHolder;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * AbstractPipe.
 *
 * @param <A> the pipe endpoint
 * @param <Q> the request packet
 * @param <R> the response packet
 */
@Beta
public abstract class AbstractPipe<A, Q, R> extends AbstractSession<A> implements Pipe<A, Q, R>, ChannelFutureListener {

  private static final class PendingPromise<Q, R> implements Comparable<PendingPromise<Q, R>> {
    private final long                        id;
    private final CompletableFuture<?>        cf;
    private final CompletablePacketContext<Q> pc;
    private       Object                      ref;

    private PendingPromise(long id, CompletableFuture<?> cf, CompletablePacketContext<Q> pc) {
      this.id = id;
      this.cf = cf;
      this.pc = pc;
    }

    void abort(Throwable cause) {
      if (!cf.isDone()) {
        cf.completeExceptionally(cause);
      }
    }

    @Override public int compareTo(@Nullable PendingPromise<Q, R> other) {
      if (other == null) {
        return 1;
      }
      return Long.compare(id, other.id);
    }
  }

  private static final AtomicLong PACKET_ID = new AtomicLong(0);

  protected final PipeContext<A, ? extends Session<A>> context;

  protected volatile Channel channel;

  protected final boolean  loggingEnabled;
  protected final LogLevel logLevel;

  private final Executor                  executor;
  private final ScheduledExecutorService  pipeExecutor;
  private final MultithreadEventLoopGroup workerGroup;

  protected final Bootstrap bootstrap;

  protected final MessageQueue<CompletablePacketContext<Q>> packetQueue;

  private final Set<PendingPromise<Q, R>> pendingPromises;

  protected final AtomicLong requestsCount = new AtomicLong(0);
  protected final AtomicLong errorsConnect = new AtomicLong(0);
  protected final AtomicLong errorsTimeout = new AtomicLong(0);

  private final AtomicReference<Future<?>> reconnectFuture = new AtomicReference<>();

  private final Map<Long, CompletableFuture<R>> cfs = new ConcurrentHashMap<>();

  protected AbstractPipe(PipeContext<A, ? extends Session<A>> context) {
    this(context, new ConcurrentLinkedDeque<>());
  }

  protected AbstractPipe(PipeContext<A, ? extends Session<A>> context, Deque<CompletablePacketContext<Q>> queue) {
    super(context.endpoint());
    this.context = context;

    loggingEnabled = Properties.instance().getBoolean(context.sessionName() + ".loggingEnabled", false);
    logLevel       = LogLevel.valueOf(Properties.instance().getString(context.sessionName() + ".logLevel", "DEBUG"));

    packetQueue = new SimpleMessageQueue<>(queue, this::sendPacket);

    pendingPromises = new ConcurrentSkipListSet<>();

    executor     = ExecutorUtils.singleThreadExecutor(context.sessionName());
    pipeExecutor = ExecutorUtils.scheduledExecutorService(context.sessionName() + "Pipe", 1);

    bootstrap   = remoteAddress(context.bootstrapFactory().create(context), context.endpoint());
    workerGroup = (MultithreadEventLoopGroup) bootstrap.config().group();
  }

  public void activate() {
    createImpl();
  }

  public void deactivate() {
    State currentState = state.get();
    if (State.DESTROYED == currentState || State.NEW == currentState) {
      return;
    }
    tryCancelScheduledFutures();
    currentState = state.updateAndGet(s -> s != State.DESTROYED ? State.IDLE : State.DESTROYED);
    if (State.IDLE == currentState) {
      fireStateEvent(State.IDLE);
      closeChannel();
    }
  }

  @Override public void operationComplete(ChannelFuture future) {
    future.removeListener(this);
    closeChannel(future.channel());

    if (state.compareAndSet(State.ACTIVE, State.IDLE)) {
      fireStateEvent(State.IDLE);
      Throwable cause = future.cause() != null ? future.cause() : EOF;
      scheduleReconnect(cause);
    }
  }

  @Override public CompletableFuture<Void> sendAsync(Q packet) {
    requestsCount.incrementAndGet();
    CompletablePacketContext<Q> packetContext = packetContext(packet);

    packetQueue.put(packetContext);
    if (state.get() == State.ACTIVE) {
      executor.execute(packetQueue::flush);
    }

    PendingPromise<Q, R> pending = new PendingPromise<>(packetContext.packetId(), packetContext.cf(), packetContext);
    addPending(pending, "sendAsync");

    return packetContext.cf();
  }

  @Override public CompletableFuture<R> sendReceiveAsync(Q packet) {
    requestsCount.incrementAndGet();
    CompletablePacketContext<Q> packetContext  = packetContext(packet);
    CompletableFuture<R>        responseFuture = new CompletableFuture<>();

    cfs.put(packetContext.packetId(), responseFuture);

    CompletableFuture<R> cf = packetContext.cf().handle((v, t) -> {
      if (t != null) {
        responseFuture.completeExceptionally(t);
      }
      return responseFuture;
    }).thenCompose(UnaryOperator.identity());

    packetQueue.put(packetContext);
    if (state.get() == State.ACTIVE) {
      executor.execute(packetQueue::flush);
    }

    PendingPromise<Q, R> pending = new PendingPromise<>(packetContext.packetId(), cf, packetContext);
    addPending(pending, "sendReceiveAsync");

    return cf;
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

  public int pendingResponses() {
    return cfs.size();
  }

  public long requestsCount() {
    return requestsCount.get();
  }

  protected void closeChannel() {
    Channel ch = this.channel;
    this.channel = null;
    closeChannel(ch);
  }

  protected void closeChannel(Channel ch) {
    packetQueue.tryInterrupt();
    if (ch != null) {
      ch.closeFuture().removeListener(this);
      ch.close().addListener(f -> Log.info().log(getClass(), "channel {} closed", ch));
    }
  }

  protected boolean connectionTimeout(Channel channel) {
    Log.debug().log(getClass(), "connection timeout event for {}, state: {}", endpoint, state.get());
    tryCancelScheduledFutures();

    if (State.DESTROYED == state.get()) {
      return true;
    }
    errorsTimeout.incrementAndGet();

    State currentState = state.updateAndGet(s -> s != State.ACTIVE ? State.TIMEOUT : State.ACTIVE);
    if (State.ACTIVE == currentState) {
      Log.debug().log(getClass(), "no timeout actions were scheduled because pipe has been established");
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

  @Override protected void createImpl() {
    if (!state.compareAndSet(State.IDLE, State.CREATING)) {
      return;
    }

    Log.debug().log(getClass(), "connecting to {}", endpoint);

    fireStateEvent(State.CREATING);

    tryCancelScheduledFutures();

    ChannelFuture connectFuture = openChannel();
    connectFuture.addListener(this::connectCallback);
  }

  protected void consume(ChannelHandlerContext ctx, PacketHolder<R> holder) {
    CompletableFuture<R> cf = cfs.remove(holder.packetId());
    if (cf == null) {
      Log.debug().log(getClass(), "received response for unregistered packet id {}", holder.packetId());
      return;
    }
    cf.complete(holder.packet());
  }

  @Override protected void destroyImpl() {
    try {
      closeChannel();
    } catch (Exception ex) {
      Log.info().log(getClass(), ex, "error while closing channel during destroy");
    }
    workerGroup.shutdownGracefully();
    ExecutorUtils.shutdown(executor);
    ExecutorUtils.shutdown(pipeExecutor);

    packetQueue.clear();

    pendingPromises.forEach(p -> p.abort(EOF));
    cfs.clear();

    Log.info().log(getClass(), "pipe {} closed", this);
  }

  protected abstract boolean initPipeline(Channel channel);

  protected ChannelFuture openChannel() {
    return bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (!Proxy.NO_PROXY.equals(context.proxy())) {
          switch (context.proxy().type()) {
            case SOCKS -> p.addLast("proxy-socks5", new Socks5ProxyHandler(context.proxy().address()));
            case HTTP -> p.addLast("proxy-http", new HttpProxyHandler(context.proxy().address()));
            default -> {
              // do nothing
            }
          }
        }
      }
    }).connect();
  }

  protected CompletablePacketContext<Q> packetContext(Q packet) {
    long packetId    = PACKET_ID.incrementAndGet();
    long expiredOnNs = clock.nanoTime() + context.queueTimeoutNs();
    return new CompletablePacketContextImpl<>(context.packetWriter(), packetId, packet, expiredOnNs, clock);
  }

  protected abstract Bootstrap remoteAddress(Bootstrap bootstrap, Endpoint<A> endpoint);

  protected void scheduleReconnect(Throwable cause) {
    if (state.get().state() >= State.DESTROYING.state() || pipeExecutor.isShutdown()) {
      return;
    }
    Log.debug().log(getClass(), cause, "reconnect to {} after {} ms.", endpoint, context.rebuildTimeoutMs());
    try {
      Future<?> previous = reconnectFuture.getAndSet(pipeExecutor.schedule(this::createImpl, context.rebuildTimeoutMs(), MILLISECONDS));
      if (previous != null) {
        previous.cancel(false);
      }
    } catch (RejectedExecutionException ex) {
      Log.warn().log(getClass(), ex, "unable to schedule reconnect to {}", endpoint);
    }
  }

  protected void sendPacket(CompletablePacketContext<Q> context) {
    Channel ch = this.channel;
    if (ch == null) {
      context.cf().completeExceptionally(StacklessClosedChannelException.newInstance(getClass(), "sendPacket"));
      return;
    }
    try {
      ChannelFuture channelFuture = ch.writeAndFlush(context);
      channelFuture.addListener(f -> handleWriteAndFlush(context, ((ChannelFuture) f).channel(), f.cause()));
    } catch (Exception ex) {
      Log.warn().log(getClass(), ex, "unable to send packet to {}", endpoint);
      context.cf().completeExceptionally(ex);
    }
  }

  @Override protected void tryCancelScheduledFutures() {
    Future<?> f = reconnectFuture.getAndSet(null);
    if (f != null) {
      f.cancel(false);
    }
  }

  private void addPending(PendingPromise<Q, R> pending, String method) {
    var cf   = pending.cf;
    var pc   = pending.pc;
    pendingPromises.add(pending);
    pending.ref = cf.whenComplete((r, t) -> {
      if (t != null) {
        packetQueue.remove(pc);
        cfs.remove(pc.packetId());
      }
      pendingPromises.remove(pending);
    });
    if (state.get().state() >= State.DESTROYING.state()) {
      pending.abort(StacklessClosedChannelException.newInstance(getClass(), method));
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

      if (!initPipeline(channel)) {
        return false;
      }
      channel.closeFuture().addListener(this);

      this.channel = channel;
      packetQueue.resume();

      if (state.compareAndSet(State.CREATING, State.ACTIVE)) {
        Log.info().log(getClass(), "pipe {} established", channel);
        errorsConnect.set(0);
        fireStateEvent(State.ACTIVE);
        return true;
      }

      this.channel = null;
      closeChannel(channel);
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

  private void handleWriteAndFlush(CompletablePacketContext<Q> context, Channel channel, Throwable cause) {
    if (cause == null) {
      context.cf().complete(null);
      return;
    }
    if (cause instanceof ConnectionTimeoutException) {
      errorsTimeout.incrementAndGet();
    }
    context.cf().completeExceptionally(cause);
    closeChannel(channel);
    if (state.compareAndSet(State.ACTIVE, State.IDLE)) {
      fireStateEvent(State.IDLE);
      scheduleReconnect(cause);
    }
  }
}
