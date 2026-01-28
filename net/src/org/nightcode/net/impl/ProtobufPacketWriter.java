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

import com.google.protobuf.Message;

import java.io.IOException;
import java.io.OutputStream;

import org.nightcode.net.PacketWriter;

/**
 * Protobuf packet writer.
 *
 * @param <P> the packet
 */
public class ProtobufPacketWriter<P extends Message> implements PacketWriter<P> {

  @Override public int size(P packet) {
    return packet.getSerializedSize();
  }

  @Override public void write(OutputStream out, P packet) throws IOException {
    packet.writeTo(out);
  }
}
