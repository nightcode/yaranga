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
 * Integer value converter.
 */
public final class IntegerValueConverter extends AbstractValueConverter<Integer> {

  @Override protected Integer fromByteArray0(byte[] input, Charset encoding) throws ConverterException {
    if (input.length != 4) {
      throw new ConverterException(format("can not convert from input '%s' to int", Hexs.hex().fromByteArray(input)));
    }
    return ((input[0] & 0xFF) << 24)
        +  ((input[1] & 0xFF) << 16)
        +  ((input[2] & 0xFF) <<  8)
        +  ((input[3] & 0xFF) <<  0);
  }

  @Override public Integer fromString0(String input) {
    return Integer.valueOf(input);
  }

  @Override protected byte[] toByteArray0(Integer input) {
    byte[] buffer = new byte[4];
    buffer[0] = (byte) ((input >>> 24) & 0xFF);
    buffer[1] = (byte) ((input >>> 16) & 0xFF);
    buffer[2] = (byte) ((input >>>  8) & 0xFF);
    buffer[3] = (byte) ((input >>>  0) & 0xFF);
    return buffer;
  }
}
