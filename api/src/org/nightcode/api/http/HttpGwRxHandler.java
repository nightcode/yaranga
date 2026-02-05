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
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import org.nightcode.api.message.Request;
import org.nightcode.common.logging.Log;
import org.nightcode.common.logging.LoggingHandler;
import org.nightcode.net.PacketReader;
import org.nightcode.net.impl.PacketHolder;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.headersFactory;
import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;

/**
 * HTTP gateway RX handler.
 */
public class HttpGwRxHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  public static final AsciiString TEXT_PLAIN = new AsciiString("text/plain; charset=utf-8");

  public static final byte[] BAD_GATEWAY = "Bad Gateway".getBytes(StandardCharsets.UTF_8);
  public static final byte[] BAD_REQUEST = "Bad Request".getBytes(StandardCharsets.UTF_8);

  private final PacketReader<Request>                                    packetReader;
  private final BiConsumer<ChannelHandlerContext, PacketHolder<Request>> inboundConsumer;

  public HttpGwRxHandler(PacketReader<Request> packetReader, BiConsumer<ChannelHandlerContext, PacketHolder<Request>> inboundConsumer) {
    this.packetReader    = packetReader;
    this.inboundConsumer = inboundConsumer;
  }

  @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LoggingHandler loggingHandler;
    HttpResponse   response;

    if (cause instanceof IOException || cause instanceof IllegalArgumentException) {
      loggingHandler = Log.debug();
      response       = createResponse(HttpResponseStatus.BAD_REQUEST, BAD_REQUEST);
    } else {
      loggingHandler = Log.warn();
      response       = createResponse(HttpResponseStatus.BAD_GATEWAY, BAD_GATEWAY);
    }

    loggingHandler.log(getClass(), cause, "exception caught, channel: {}", ctx.channel());
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  @Override public void channelInactive(ChannelHandlerContext ctx) {
    ctx.fireChannelInactive();
  }

  @Override protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
    long packetId = Long.parseLong(fullHttpRequest.headers().get("X-Packet-ID", "-1"));

    ByteBuf msg = fullHttpRequest.content();

    final byte[] array;
    final int    offset;
    final int    msgLength = msg.readableBytes();
    if (msg.hasArray()) {
      array  = msg.array();
      offset = msg.arrayOffset() + msg.readerIndex();
    } else {
      array  = ByteBufUtil.getBytes(msg, msg.readerIndex(), msgLength, false);
      offset = 0;
    }

    Request message;
    try {
      message = packetReader.read(array, offset, msgLength);
    } catch (IOException ex) {
      throw new IllegalArgumentException("unable to build Message with supplied buffer", ex);
    }
    inboundConsumer.accept(ctx, new PacketHolder<>(packetId, message));
  }

  private FullHttpResponse createResponse(HttpResponseStatus status, byte[] content) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content)
        , headersFactory().withValidation(false), trailersFactory().withValidation(false));
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN);
    return response;
  }
}
