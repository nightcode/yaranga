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
import java.util.List;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.nightcode.net.impl.PacketHolder;
import org.nightcode.common.logging.Log;

import static org.nightcode.net.PacketContext.MAGIC;
import static org.nightcode.net.PacketContext.PACKET_PREFIX_SIZE;
import static org.nightcode.net.RawVariant32Util.readRawVariant32;

/**
 * Packet RX handler.
 *
 * @param <P> the packet
 */
public class PacketRxHandler<P> extends ByteToMessageDecoder {

  private enum State {
    READ_HEADER,
    READ_PACKET_LENGTH,
    READ_PACKET,
    CLOSED
  }

  private State state;
  private long  packetId;
  private int   packetLength;

  private final PacketReader<P>                                    packetReader;
  private final BiConsumer<ChannelHandlerContext, PacketHolder<P>> rxConsumer;

  public PacketRxHandler(PacketReader<P> packetReader, BiConsumer<ChannelHandlerContext, PacketHolder<P>> rxConsumer) {
    this.packetReader = packetReader;
    this.rxConsumer   = rxConsumer;
    this.state        = State.READ_HEADER;
  }

  @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof IOException) {
      Log.debug().log(getClass(), "exception caught", cause);
    } else {
      Log.warn().log(getClass(), "exception caught", cause);
    }
    ctx.close();
  }

  @Override public void channelInactive(ChannelHandlerContext ctx) {
    state = State.CLOSED;
    ctx.fireChannelInactive();
  }

  @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
    if (state == State.CLOSED) {
      in.skipBytes(in.readableBytes());
      return;
    }

    while (true) {
      switch (state) {
        case READ_HEADER -> {
          if (in.readableBytes() < PACKET_PREFIX_SIZE) {
            return;
          }
          int magic = in.readInt();
          if (magic != MAGIC) {
            throw new IOException("invalid packet header");
          }
          packetId = in.readLong();

          state = State.READ_PACKET_LENGTH;
        }
        case READ_PACKET_LENGTH -> {
          in.markReaderIndex();

          int preIndex = in.readerIndex();
          packetLength = readRawVariant32(in);
          if (preIndex == in.readerIndex()) {
            return;
          }
          if (packetLength < 0) {
            throw new CorruptedFrameException("negative length: " + packetLength);
          }

          state = State.READ_PACKET;
        }
        case READ_PACKET -> {
          if (in.readableBytes() < packetLength) {
            return;
          } else {
            ByteBuf buf = in.readRetainedSlice(packetLength);
            try {
              final byte[] array;
              final int    offset;
              final int    packetLength = buf.readableBytes();
              if (buf.hasArray()) {
                array  = buf.array();
                offset = buf.arrayOffset() + buf.readerIndex();
              } else {
                array  = ByteBufUtil.getBytes(buf, buf.readerIndex(), packetLength, false);
                offset = 0;
              }

              P packet;
              try {
                packet = packetReader.read(array, offset, packetLength);
              } catch (IOException ex) {
                throw new IllegalArgumentException("unable to build Packet with supplied buffer", ex);
              }
              rxConsumer.accept(ctx, new PacketHolder<>(packetId, packet));
              state             = State.READ_HEADER;
              packetId          = -1;
              this.packetLength = -1;
            } finally {
              buf.release();
            }
          }
        }
        default -> throw new IllegalStateException("illegal state: " + state);
      }
    }
  }
}
