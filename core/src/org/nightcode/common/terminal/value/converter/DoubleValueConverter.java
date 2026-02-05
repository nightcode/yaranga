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
 * Double value converter.
 */
public final class DoubleValueConverter extends AbstractValueConverter<Double> {

  private final LongValueConverter longParameterConverter = new LongValueConverter();

  @Override protected Double fromByteArray0(byte[] input, Charset encoding) throws ConverterException {
    if (input.length != 8) {
      throw new ConverterException(format("can not convert from input '%s' to double", Hexs.hex().fromByteArray(input)));
    }
    return Double.longBitsToDouble(longParameterConverter.fromByteArray0(input, encoding));
  }

  @Override public Double fromString0(String input) {
    return Double.valueOf(input);
  }

  @Override protected byte[] toByteArray0(Double input) {
    return longParameterConverter.toByteArray0(Double.doubleToLongBits(input));
  }
}
