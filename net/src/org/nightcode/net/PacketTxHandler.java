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

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import org.nightcode.common.net.ConnectionTimeoutException;
import org.nightcode.common.logging.Log;

import static java.lang.String.format;
import static org.nightcode.net.PacketContext.MAGIC;
import static org.nightcode.net.PacketContext.PACKET_PREFIX_SIZE;
import static org.nightcode.net.RawVariant32Util.computeRawVariant32Size;
import static org.nightcode.net.RawVariant32Util.writeRawVariant32;

/**
 * Packet TX handler.
 *
 * @param <P> the packet
 */
public class PacketTxHandler<P> extends ChannelDuplexHandler {

  @Override public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
    ctx.close(promise);
  }

  @Override public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) {
    try {
      if (!(packet instanceof PacketContext)) {
        promise.tryFailure(new UnsupportedMessageTypeException(format("illegal packet %s, MUST be instance of %s"
            , packet.getClass().getName(), PacketContext.class)));
        return;
      }

      // noinspection unchecked
      PacketContext<P> context = (PacketContext<P>) packet;
      if (context.isExpired()) {
        promise.tryFailure(new ConnectionTimeoutException("packet " + context.packetId() + " expired, channel: " + ctx.channel().id()));
        return;
      }

      write0(ctx, context, promise);
    } catch (Exception ex) {
      exceptionCaught(ctx, ex);
      promise.tryFailure(ex);
    }
  }

  @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof IOException) {
      Log.debug().log(getClass(), cause, "exception caught, channel: {}", ctx.channel());
    } else {
      Log.warn().log(getClass(), cause, "exception caught, channel: {}", ctx.channel());
    }
    ctx.close();
  }

  private void write0(ChannelHandlerContext ctx, PacketContext<P> context, ChannelPromise promise) {
    ByteBuf out = null;
    try {
      int bodyLen   = context.serializedSize();
      int headerLen = computeRawVariant32Size(bodyLen);

      out = ctx.alloc().ioBuffer(PACKET_PREFIX_SIZE + headerLen + bodyLen);
      out.writeInt(MAGIC);
      out.writeLong(context.packetId());
      writeRawVariant32(out, bodyLen);
      context.writeTo(new ByteBufOutputStream(out));

      if (out.isReadable()) {
        ctx.write(out, promise);
      } else {
        out.release();
        ctx.write(Unpooled.EMPTY_BUFFER, promise);
      }
      out = null;
    } catch (EncoderException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new EncoderException(ex);
    } finally {
      if (out != null) {
        out.release();
      }
    }
  }
}
