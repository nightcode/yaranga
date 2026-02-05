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

import java.io.IOException;
import java.net.URI;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.nightcode.api.message.Request;
import org.nightcode.common.logging.Log;
import org.nightcode.common.logging.LoggingHandler;
import org.nightcode.net.ConnectionTimeoutException;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.props.Properties;
import org.nightcode.net.PacketContext;

import static java.lang.String.format;

/**
 * HTTP TX handler.
 */
public class HttpTxHandler extends ChannelDuplexHandler {

  private static final String USER_AGENT = Properties.instance().getString("org.nightcode.api.http.UserAgent", "nightcode-api/0.1");

  private final Endpoint<URI> endpoint;

  public HttpTxHandler(Endpoint<URI> endpoint) {
    this.endpoint = endpoint;
  }

  @Override public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
    ctx.close(promise);
  }

  @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    try {
      if (!(msg instanceof PacketContext)) {
        promise.tryFailure(new UnsupportedMessageTypeException(format("illegal packet %s, MUST be instance of %s"
            , msg.getClass().getName(), PacketContext.class)));
        return;
      }

      // noinspection unchecked
      PacketContext<Request> context = (PacketContext<Request>) msg;
      if (context.isExpired()) {
        promise.tryFailure(new ConnectionTimeoutException("message " + context.packetId() + " expired, channel: " + ctx.channel().id()));
        return;
      }

      write0(ctx, context, promise);
    } catch (Exception ex) {
      exceptionCaught(ctx, ex);
      promise.tryFailure(ex);
    }
  }

  @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LoggingHandler loggingHandler = (cause instanceof IOException) ? Log.debug() : Log.warn();
    loggingHandler.log(getClass(), cause, "exception caught, channel: {}", ctx.channel());
    ctx.close();
  }

  private void write0(ChannelHandlerContext ctx, PacketContext<Request> context, ChannelPromise promise) {
    ByteBuf content = null;
    try {
      int bodyLen = context.serializedSize();

      content = ctx.alloc().ioBuffer(bodyLen);
      context.writeTo(new ByteBufOutputStream(content));

      FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, endpoint.resolve().toASCIIString()
          , content);
      request.headers().set(HttpHeaderNames.HOST, endpoint.resolve().getHost());
      request.headers().set(HttpHeaderNames.USER_AGENT, USER_AGENT);
      request.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
      request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/protobuf");
      request.headers().set("X-Packet-ID", context.packetId());

      if (content.isReadable()) {
        ctx.write(request, promise);
      } else {
        content.release();
        ctx.write(Unpooled.EMPTY_BUFFER, promise);
      }
      content = null;
    } catch (EncoderException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new EncoderException(ex);
    } finally {
      if (content != null) {
        content.release();
      }
    }
  }
}
