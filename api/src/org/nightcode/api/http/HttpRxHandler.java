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
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.nightcode.api.ApiException;
import org.nightcode.api.message.Response;
import org.nightcode.common.logging.Log;
import org.nightcode.common.logging.LoggingHandler;
import org.nightcode.net.PacketReader;
import org.nightcode.net.impl.PacketHolder;

/**
 * HTTP RX handler.
 */
public class HttpRxHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

  public static final int SC_MULTIPLE_CHOICES = 300;
  public static final int SC_OK               = 200;

  private final PacketReader<Response>                                    packetReader;
  private final BiConsumer<ChannelHandlerContext, PacketHolder<Response>> inboundConsumer;

  public HttpRxHandler(PacketReader<Response> packetReader, BiConsumer<ChannelHandlerContext, PacketHolder<Response>> inboundConsumer) {
    this.packetReader    = packetReader;
    this.inboundConsumer = inboundConsumer;
  }

  @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LoggingHandler loggingHandler = (cause instanceof IOException) ? Log.debug() : Log.warn();
    loggingHandler.log(getClass(), "exception caught", cause);
    ctx.close();
  }

  @Override public void channelInactive(ChannelHandlerContext ctx) {
    ctx.fireChannelInactive();
  }

  @Override protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse fullHttpResponse) {
    long packetId = Long.parseLong(fullHttpResponse.headers().get("X-Packet-ID", "-1"));

    if (!isSuccess(fullHttpResponse.status().code())) {
      throw new ApiException(fullHttpResponse.status().code(), fullHttpResponse.status().reasonPhrase());
    }

    ByteBuf msg = fullHttpResponse.content();

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

    Response message;
    try {
      message = packetReader.read(array, offset, msgLength);
    } catch (IOException ex) {
      throw new IllegalArgumentException("unable to build Message with supplied buffer", ex);
    }
    inboundConsumer.accept(ctx, new PacketHolder<>(packetId, message));
  }

  private boolean isSuccess(int statusCode) {
    return statusCode >= SC_OK && statusCode < SC_MULTIPLE_CHOICES;
  }
}
