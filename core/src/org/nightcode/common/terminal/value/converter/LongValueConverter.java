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

import java.nio.charset.Charset;

import org.nightcode.common.base.Hexs;

import static java.lang.String.format;

/**
 * Long value converter.
 */
public final class LongValueConverter extends AbstractValueConverter<Long> {

  @Override protected Long fromByteArray0(byte[] input, Charset encoding) throws ConverterException {
    if (input.length != 8) {
      throw new ConverterException(format("can not convert from input '%s' to long", Hexs.hex().fromByteArray(input)));
    }
    return ((long) (input[0] & 0xFF) << 56)
        +  ((long) (input[1] & 0xFF) << 48)
        +  ((long) (input[2] & 0xFF) << 40)
        +  ((long) (input[3] & 0xFF) << 32)
        +  ((long) (input[4] & 0xFF) << 24)
        +  ((long) (input[5] & 0xFF) << 16)
        +  ((long) (input[6] & 0xFF) <<  8)
        +  ((long) (input[7] & 0xFF) <<  0);
  }

  @Override public Long fromString0(String input) {
    return Long.valueOf(input);
  }

  @Override protected byte[] toByteArray0(Long input) {
    byte[] buffer = new byte[8];
    buffer[0] = (byte) (input >>> 56);
    buffer[1] = (byte) (input >>> 48);
    buffer[2] = (byte) (input >>> 40);
    buffer[3] = (byte) (input >>> 32);
    buffer[4] = (byte) (input >>> 24);
    buffer[5] = (byte) (input >>> 16);
    buffer[6] = (byte) (input >>>  8);
    buffer[7] = (byte) (input >>>  0);
    return buffer;
  }
}
