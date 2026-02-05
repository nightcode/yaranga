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

package org.nightcode.common.terminal.value.converter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * UUID value converter.
 */
public final class UuidValueConverter extends AbstractValueConverter<UUID> {

  @Override protected UUID fromByteArray0(byte[] input, Charset encoding) {
    ByteBuffer buffer     = ByteBuffer.wrap(input);
    long       firstLong  = buffer.getLong();
    long       secondLong = buffer.getLong();
    return new UUID(firstLong, secondLong);
  }

  @Override protected UUID fromString0(String input) {
    return UUID.fromString(input);
  }

  @Override protected byte[] toByteArray0(UUID input) {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
    buffer.putLong(input.getMostSignificantBits());
    buffer.putLong(input.getLeastSignificantBits());
    return buffer.array();
  }

  @Override protected String toString0(UUID input) {
    return input.toString();
  }
}
