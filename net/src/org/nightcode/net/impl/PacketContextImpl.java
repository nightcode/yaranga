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

import java.io.IOException;
import java.io.OutputStream;

import org.nightcode.common.util.Clock;
import org.nightcode.net.PacketContext;
import org.nightcode.net.PacketWriter;

/**
 * todo.
 *
 * @param <P> the packet
 */
public class PacketContextImpl<P> implements PacketContext<P> {

  protected final PacketWriter<P> packetWriter;
  protected final long            packetId;
  protected final P               packet;
  protected final long            expiredOnNs;

  private final Clock clock;

  public PacketContextImpl(PacketWriter<P> packetWriter, long packetId, P packet) {
    this(packetWriter, packetId, packet, Long.MAX_VALUE);
  }

  public PacketContextImpl(PacketWriter<P> packetWriter, long packetId, P packet, long expiredOnNs) {
    this(packetWriter, packetId, packet, expiredOnNs, Clock.sys());
  }

  public PacketContextImpl(PacketWriter<P> packetWriter, long packetId, P packet, long expiredOnNs, Clock clock) {
    this.packetWriter = packetWriter;
    this.packetId     = packetId;
    this.packet       = packet;
    this.expiredOnNs  = expiredOnNs;
    this.clock        = clock;
  }

  @Override public boolean isExpired() {
    return expiredOnNs < clock.nanoTime();
  }

  @Override public P packet() {
    return packet;
  }

  @Override public long packetId() {
    return packetId;
  }

  @Override public int serializedSize() {
    return packetWriter.size(packet);
  }

  @Override public void writeTo(OutputStream out) throws IOException {
    packetWriter.write(out, packet);
  }
}
