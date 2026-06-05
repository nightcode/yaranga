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

package org.nightcode.api;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.api.message.Status;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;
import org.nightcode.common.service.AbstractService;
import org.nightcode.common.util.Closeables;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.net.BootstrapServerFactory;
import org.nightcode.net.PacketContext;
import org.nightcode.net.PacketReader;
import org.nightcode.net.PacketWriter;
import org.nightcode.net.impl.PacketContextImpl;
import org.nightcode.net.impl.PacketHolder;
import org.nightcode.net.impl.ProtobufPacketReader;
import org.nightcode.net.impl.ProtobufPacketWriter;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Abstract API gateway.
 */
public abstract class ApiGw extends AbstractService implements ChannelFutureListener, Closeable {

  private record InterceptApiGwContext(ApiGwContext delegate, ApiGwInterceptor interceptor) implements ApiGwContext {

    @Override public void close() {
      delegate.close();
    }

    @Override public int maxBodyLengthBytes() {
      return delegate.maxBodyLengthBytes();
    }

    @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> newApiCall() {
      return interceptor.intercept(delegate);
    }

    @Override public ConcurrentMap<String, ApiHandler> apiHandlers() {
      return delegate.apiHandlers();
    }
  }

  private static final long RECONNECT_TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(500);

  private static Response createErrorResponse(String serviceName, int errorCode, String errorMessage) {
    return Response.newBuilder()
        .setService(serviceName)
        .setError(Status.newBuilder()
            .setCode(errorCode)
            .setMessage(errorMessage)
        ).build();
  }

  private static ApiGwContext intercept(ApiGwContext context, List<ApiGwInterceptor> interceptors) {
    for (ApiGwInterceptor interceptor : interceptors) {
      context = new InterceptApiGwContext(context, interceptor);
    }
    return context;
  }

  private volatile ChannelFuture channelFuture;

  private final String                    name;
  private final SslContext                sslContext;
  private final Function<Request, String> serviceNameProvider;

  protected final ApiGwContext context;

  private final boolean  loggingEnabled;
  private final LogLevel logLevel;

  private final BootstrapServerFactory<?> serverFactory;

  private final ServerBootstrap          serverBootstrap;
  private final ScheduledExecutorService executor;

  private volatile boolean closing = false;

  private final AtomicLong requestsCount = new AtomicLong(0);

  protected final PacketReader<Request>  packetReader = new ProtobufPacketReader<>(Request.getDefaultInstance());
  private final   PacketWriter<Response> packetWriter = new ProtobufPacketWriter<>();

  protected ApiGw(ApiGwBuilder builder) {
    name                = builder.name;
    sslContext          = builder.sslContext;
    serviceNameProvider = builder.serviceNameProvider;
    serverFactory       = builder.bootstrapFactory;

    ApiGwContext context = new ApiGwContextImpl(builder.maxBodyLengthBytes);

    Log.info().log(getClass(), "interceptors: {}", builder.interceptors);
    this.context = intercept(context, builder.interceptors);

    loggingEnabled = Properties.instance().getBoolean(name + ".loggingEnabled", false);
    logLevel       = LogLevel.valueOf(Properties.instance().getString(name + ".logLevel", "DEBUG"));

    executor = ExecutorUtils.scheduledExecutorService(name + "Reconnect", 1);

    int nThreads = Runtime.getRuntime().availableProcessors();

    serverBootstrap = serverFactory.create(name, nThreads);
  }

  public void addApiHandler(ApiHandler apiHandler) {
    addApiHandler(apiHandler.name(), apiHandler);
  }

  public void addApiHandler(String name, ApiHandler apiHandler) {
    if (closing) {
      return;
    }
    Log.debug().log(getClass(), "added ApiHandler {}", apiHandler.name());
    context.apiHandlers().put(name, apiHandler);
  }

  @Override public void close() {
    stopAsync().join();
  }

  public SocketAddress localAddress() {
    return serverFactory.localAddress();
  }

  public String name() {
    return name;
  }

  @Override public void operationComplete(ChannelFuture future) {
    future.removeListener(this);
    future.channel().close();
    if (!closing) {
      executor.schedule(this::bind, RECONNECT_TIMEOUT_NS, NANOSECONDS);
    }
  }

  public boolean withSsl() {
    return sslContext != null;
  }

  @Override protected void doStart() {
    try {
      bind();
      notifyStarted();
    } catch (Exception ex) {
      notifyFailed(ex);
    }
  }

  @Override protected void doStop() {
    try {
      closing = true;

      ChannelFuture tmpChannelFuture = channelFuture;
      if (tmpChannelFuture != null) {
        tmpChannelFuture.channel().closeFuture().removeListener(this);
        tmpChannelFuture.channel().close();
      }
      Closeables.close(context);
      channelFuture = null;
      ExecutorUtils.shutdown(executor);
      ExecutorUtils.shutdown(serverBootstrap.config().group());
      ExecutorUtils.shutdown(serverBootstrap.config().childGroup());
      notifyStopped();
    } catch (Exception ex) {
      notifyFailed(ex);
    }
  }

  protected abstract Consumer<ChannelPipeline> pipelineConsumer();

  long requestsCount() {
    return requestsCount.get();
  }

  MultithreadEventExecutorGroup acceptorGroup() {
    return (MultithreadEventExecutorGroup) serverBootstrap.config().group();
  }

  MultithreadEventExecutorGroup workerGroup() {
    return (MultithreadEventExecutorGroup) serverBootstrap.config().childGroup();
  }

  protected void bind() {
    try {
      ChannelInitializer<Channel> initializer = new ChannelInitializer<>() {
        @Override protected void initChannel(Channel ch) {
          ChannelPipeline p = ch.pipeline();
          if (sslContext != null) {
            p.addLast("ssl", sslContext.newHandler(ch.alloc()));
          }
          if (loggingEnabled) {
            p.addLast("logger", new LoggingHandler(name, logLevel));
          }

          pipelineConsumer().accept(p);
        }
      };
      channelFuture = serverBootstrap.childHandler(initializer).bind().sync();
      channelFuture.channel().closeFuture().addListener(this);
      Log.info().log(getClass(), "{} listening on address: {}, use SSL: {}"
          , channelFuture.channel(), serverFactory.localAddress(), sslContext != null);
    } catch (Exception ex) {
      Log.warn().log(getClass(), ex, "unable bind to {}, will try again after {} ms."
          , serverFactory.localAddress(), NANOSECONDS.toMillis(RECONNECT_TIMEOUT_NS));
      executor.schedule(this::bind, RECONNECT_TIMEOUT_NS, NANOSECONDS);
    }
  }

  protected <Q extends Message, R extends Message> void consume(ChannelHandlerContext ctx, PacketHolder<Request> holder) {
    requestsCount.incrementAndGet();

    Request         request    = holder.packet();
    ApiGwCall<Q, R> serverCall = context.newApiCall();

    CompletableFuture<Void> execution = new CompletableFuture<>();
    execution
        .thenCompose(v -> execute(request, serverCall))
        .handle((r, t) -> {
          if (t != null) {
            if (t instanceof CompletionException && t.getCause() != null) {
              t = t.getCause();
            }
            if (t instanceof ApiException apiException) {
              Log.info().log(getClass(), "{} {}", ctx.channel(), apiException.getMessage());
              return createErrorResponse(request.getService(), apiException.statusCode(), apiException.getMessage());
            }
            Log.error().log(getClass(), t, "{} an unexpected exception occurred", ctx.channel());
            return createErrorResponse(request.getService(), 500, "Internal Server Error");
          }

          return Response.newBuilder()
              .setService(request.getService())
              .setContent(Any.pack(r))
              .build();
        })
        .thenAccept(r -> send(ctx, holder, r));

    execution.complete(null);
  }

  private <Q extends Message, R extends Message> CompletableFuture<R> execute(Request request, ApiGwCall<Q, R> serverCall) {
    String serviceName = serviceNameProvider.apply(request);

    ApiHandler apiHandler = context.apiHandlers().get(serviceName);
    if (apiHandler == null) {
      return CompletableFuture.failedFuture(new ApiException(404, format("service '%s' Not Found", serviceName)));
    }

    Any    payload = request.getPayload();
    String typeUrl = payload.getTypeUrl();
    // noinspection unchecked
    Class<Q> requestClass = (Class<Q>) apiHandler.findClass(typeUrl);
    if (requestClass == null) {
      return CompletableFuture.failedFuture(new ApiException(404, format("request class '%s' Not Found", typeUrl)));
    }

    Q requestContent;
    try {
      requestContent = payload.unpack(requestClass);
    } catch (InvalidProtocolBufferException ex) {
      return CompletableFuture.failedFuture(new ApiException(400, format("service '%s', Bad Request content '%s' "
          , serviceName, ex.getMessage())));
    }

    MethodHandler<Q, R> methodHandler = apiHandler.handlerFor(typeUrl);
    if (methodHandler == null) {
      return CompletableFuture.failedFuture(new ApiException(404, format("service method '%s' Not Found", typeUrl)));
    }

    return serverCall.executeAsync(serviceName, methodHandler, requestContent, request.getMetadata());
  }

  private void handleWriteAndFlush(PacketContext<Response> context, ChannelFuture future) {
    Throwable cause = future.cause();
    if (cause == null) {
      return;
    }
    Log.info().log(getClass(), "unable to send packet with id {}", context.packetId());
    future.channel().close();
  }

  private void send(ChannelHandlerContext ctx, PacketHolder<Request> holder, Response response) {
    PacketContext<Response> responseContext = new PacketContextImpl<>(packetWriter, holder.packetId(), response);
    try {
      ChannelFuture channelFuture = ctx.writeAndFlush(responseContext);
      channelFuture.addListener(f -> handleWriteAndFlush(responseContext, (ChannelFuture) f));
    } catch (Exception ex) {
      Log.info().log(getClass(), "unable to send packet with id {}", holder.packetId());
    }
  }
}
