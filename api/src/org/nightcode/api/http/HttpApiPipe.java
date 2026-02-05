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

package org.nightcode.api.http;

import java.net.Proxy;
import java.net.URI;
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
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.nightcode.api.ApiPipe;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.common.logging.Log;
import org.nightcode.net.ConnectionTimeoutException;
import org.nightcode.common.net.MessageQueue;
import org.nightcode.common.net.SimpleMessageQueue;
import org.nightcode.common.pool.AbstractSession;
import org.nightcode.common.pool.Session;
import org.nightcode.common.props.Properties;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.net.CompletablePacketContext;
import org.nightcode.net.PacketFuture;
import org.nightcode.net.PipeContext;
import org.nightcode.net.impl.CompletablePacketContextImpl;
import org.nightcode.net.impl.PacketHolder;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * HTTP connection.
 */
public class HttpApiPipe extends AbstractSession<URI> implements ApiPipe<URI>, ChannelFutureListener {

  private static final AtomicLong PACKET_ID = new AtomicLong(1);

  private final PipeContext<URI, ? extends Session<URI>> context;

  private final boolean  loggingEnabled;
  private final LogLevel logLevel;

  private volatile Channel   channel;
  private volatile Future<?> reconnectFuture;

  private final ExecutorService           executor;
  private final ScheduledExecutorService  connectionExecutor;
  private final MultithreadEventLoopGroup workerGroup;

  private final String host;
  private final int    port;

  private final Bootstrap bootstrap;

  private final Deque<CompletablePacketContext<Request>>        queue;
  private final MessageQueue<CompletablePacketContext<Request>> packetQueue;

  private final   AtomicLong requestsCount = new AtomicLong(0);
  protected final AtomicLong errorsConnect = new AtomicLong(0);
  protected final AtomicLong errorsTimeout = new AtomicLong(0);

  private final ConcurrentMap<Long, CompletableFuture<Response>> cfs = new ConcurrentHashMap<>();

  protected HttpApiPipe(PipeContext<URI, ? extends Session<URI>> context) {
    this(context, new ConcurrentLinkedDeque<>());
  }

  protected HttpApiPipe(PipeContext<URI, ? extends Session<URI>> context, Deque<CompletablePacketContext<Request>> queue) {
    super(context.endpoint());
    this.context = context;
    this.queue   = queue;

    host = endpoint().resolve().getHost();
    if (endpoint.resolve().getPort() == -1) {
      port = "http".equalsIgnoreCase(endpoint.resolve().getScheme()) ? 80 : 443;
    } else {
      port = endpoint.resolve().getPort();
    }

    loggingEnabled = Properties.instance().getBoolean(context.sessionName() + ".loggingEnabled", false);
    logLevel       = LogLevel.valueOf(Properties.instance().getString(context.sessionName() + ".logLevel", "DEBUG"));

    packetQueue = new SimpleMessageQueue<>(queue, this::sendMessage);

    executor           = ExecutorUtils.singleThreadExecutor(context.sessionName());
    connectionExecutor = ExecutorUtils.scheduledExecutorService(context.sessionName() + "Pipe", 1);

    bootstrap   = context.bootstrapFactory().create(context);
    workerGroup = (MultithreadEventLoopGroup) bootstrap.config().group();
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
    ExecutorUtils.shutdown(connectionExecutor);

    try {
      closeChannel(channel);
      channel = null;
    } catch (Exception ex) {
      Log.info().log(getClass(), ex);
    }
    workerGroup.shutdownGracefully();

    queue.clear();
    for (Long packetId : cfs.keySet()) {
      CompletableFuture<Response> cf = cfs.remove(packetId);
      cf.completeExceptionally(EOF);
    }
    Log.warn().log(getClass(), "connection {} closed", this);
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

    ChannelFuture checkFuture = bootstrap.handler(new ChannelInitializer<SocketChannel>() {
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
    }).connect(host, port);
    checkFuture.addListener(this::connectCallback);
  }

  @Override public CompletableFuture<Void> sendAsync(Request message) {
    requestsCount.incrementAndGet();
    CompletablePacketContext<Request> messageContext = packetContext(message);

    packetQueue.put(messageContext);
    if (State.ACTIVE == state.get()) {
      executor.execute(packetQueue::flush);
    }

    messageContext.completion().whenComplete((v, t) -> {
      if (t != null) {
        packetQueue.remove(messageContext);
      }
    });
    return messageContext.completion();
  }

  @Override public CompletableFuture<Response> sendReceiveAsync(Request request) {
    requestsCount.incrementAndGet();
    CompletablePacketContext<Request> packetContext  = packetContext(request);
    CompletableFuture<Response>       responseFuture = new PacketFuture<>(packetContext.packetId(), (id, cause) -> cfs.remove(id));

    cfs.put(packetContext.packetId(), responseFuture);

    packetQueue.put(packetContext);
    if (State.ACTIVE == state.get()) {
      executor.execute(packetQueue::flush);
    }

    CompletableFuture<Response> cf = packetContext.completion().handle((v, t) -> {
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
      Log.debug().log(getClass(), "connection to {} was not established", endpoint);
    }
  }

  private boolean connectCallback(Channel channel, Throwable cause) {
    if (State.DESTROYED == state.get()) {
      closeChannel(channel);
      return false;
    }

    if (cause == null) {
      tryCancelScheduledFutures();

      ChannelPipeline pipeline = channel.pipeline();
      if (context.sslContext() != null) {
        pipeline.addLast("ssl", context.sslContext().newHandler(channel.alloc(), host, port));
      }
      if (loggingEnabled) {
        pipeline.addLast("logger", new LoggingHandler(context.sessionName(), logLevel));
      }

      HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
          .setMaxInitialLineLength(4096)
          .setMaxHeaderSize(8192)
          .setMaxChunkSize(8192)
          .setChunkedSupported(true)
          .setValidateHeaders(false);

      pipeline.addLast("decoder", new HttpResponseDecoder(httpDecoderConfig));
      pipeline.addLast("encoder", new HttpRequestEncoder() {
        @Override protected void sanitizeHeadersBeforeEncode(HttpRequest msg, boolean isAlwaysEmpty) {
          String acceptEncoding = msg.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
          if (acceptEncoding != null && acceptEncoding.contains("br")) {
            msg.headers().remove(HttpHeaderNames.ACCEPT_ENCODING);
            msg.headers().add(HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate");
          }
          super.sanitizeHeadersBeforeEncode(msg, isAlwaysEmpty);
        }
      });

      pipeline.addLast("inflater", new HttpContentDecompressor(0));
      pipeline.addLast("aggregator", new LoggableHttpObjectAggregator(context.maxBodyLengthBytes()));

      pipeline.addLast("tx", new HttpTxHandler(endpoint));
      pipeline.addLast("rx", new HttpRxHandler(context.packetReader(), this::consume));
      channel.closeFuture().addListener(this);

      this.channel = channel;
      packetQueue.resume();

      if (state.compareAndSet(State.CREATING, State.ACTIVE)) {
        Log.info().log(getClass(), "connection {} established", channel);
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
      Log.debug().log(getClass(), () -> "no timeout actions were scheduled because connection has been established");
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

  private void consume(ChannelHandlerContext ctx, PacketHolder<Response> holder) {
    CompletableFuture<Response> cf = cfs.remove(holder.packetId());
    if (cf == null) {
      Log.debug().log(getClass(), "unable to find consumer for packet with id {}", holder.packetId());
      return;
    }
    cf.complete(holder.packet());
  }

  private void handleWriteAndFlush(CompletablePacketContext<Request> context, Channel channel, Throwable cause) {
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

  private CompletablePacketContext<Request> packetContext(Request message) {
    long messageId   = PACKET_ID.incrementAndGet();
    long expiredOnNs = clock.nanoTime() + context.queueTimeoutNs();
    return new CompletablePacketContextImpl<>(context.packetWriter(), messageId, message, expiredOnNs, clock);
  }

  private void scheduleReconnect(Throwable cause) {
    Log.debug().log(getClass(), cause, "reconnect to {} after {} ms.", endpoint, context.rebuildTimeoutMs());
    reconnectFuture = connectionExecutor.schedule(this::createImpl, context.rebuildTimeoutMs(), MILLISECONDS);
  }

  private void sendMessage(CompletablePacketContext<Request> context) {
    try {
      ChannelFuture channelFuture = channel.writeAndFlush(context);
      channelFuture.addListener(f -> handleWriteAndFlush(context, ((ChannelFuture) f).channel(), f.cause()));
    } catch (Exception ex) {
      Log.warn().log(getClass(), ex, "unable to send message to {}", endpoint);
      context.completion().completeExceptionally(ex);
    }
  }
}
