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
 * Float value converter.
 */
public final class FloatValueConverter extends AbstractValueConverter<Float> {

  private final IntegerValueConverter integerParameterConverter = new IntegerValueConverter();

  @Override protected Float fromByteArray0(byte[] input, Charset encoding) throws ConverterException {
    if (input.length != 4) {
      throw new ConverterException(format("can not convert from input '%s' to float", Hexs.hex().fromByteArray(input)));
    }
    return Float.intBitsToFloat(integerParameterConverter.fromByteArray0(input, encoding));
  }

  @Override public Float fromString0(String input) {
    return Float.valueOf(input);
  }

  @Override protected byte[] toByteArray0(Float input) {
    return integerParameterConverter.toByteArray0(Float.floatToIntBits(input));
  }
}
