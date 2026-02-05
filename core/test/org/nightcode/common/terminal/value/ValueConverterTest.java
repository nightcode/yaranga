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

package org.nightcode.common.terminal.value;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.nightcode.common.terminal.value.converter.ConverterException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ValueConverter}.
 */
public class ValueConverterTest {

  
  @Test public void bigDecimalConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<BigDecimal> converter = (ValueConverter<BigDecimal>) ValueConverterService.def().getConverter(BigDecimal.class);
    convert(BigDecimal.TEN, converter);
  }

  @Test public void booleanConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Boolean> converter = (ValueConverter<Boolean>) ValueConverterService.def().getConverter(Boolean.class);
    convert(Boolean.TRUE, converter);
  }

  @Test public void byteConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Byte> converter = (ValueConverter<Byte>) ValueConverterService.def().getConverter(Byte.class);
    convert(Byte.MAX_VALUE, converter);
  }

  @Test public void doubleConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Double> converter = (ValueConverter<Double>) ValueConverterService.def().getConverter(Double.class);
    convert(Double.MAX_VALUE, converter);
  }

  @Test public void floatConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Float> converter = (ValueConverter<Float>) ValueConverterService.def().getConverter(Float.class);
    convert(Float.MAX_VALUE, converter);
  }

  @Test public void integerConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Integer> converter = (ValueConverter<Integer>) ValueConverterService.def().getConverter(Integer.class);
    convert(Integer.MAX_VALUE, converter);
  }

  @Test public void longConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Long> converter = (ValueConverter<Long>) ValueConverterService.def().getConverter(Long.class);
    convert(Long.MAX_VALUE, converter);
  }

  @Test public void shortConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<Short> converter = (ValueConverter<Short>) ValueConverterService.def().getConverter(Short.class);
    convert(Short.MAX_VALUE, converter);
  }

  @Test public void stringConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<String> converter = (ValueConverter<String>) ValueConverterService.def().getConverter(String.class);
    convert("test string", converter);
  }

  @Test public void uuidConvert() throws ConverterException {
    // noinspection unchecked
    ValueConverter<UUID> converter = (ValueConverter<UUID>) ValueConverterService.def().getConverter(UUID.class);
    convert(UUID.randomUUID(), converter);
  }

  @Test public void voidConvert() throws Exception {
    // noinspection unchecked
    ValueConverter<Void> converter = (ValueConverter<Void>) ValueConverterService.def().getConverter(Void.class);

    Constructor<Void> constructor = Void.class.getDeclaredConstructor();
    try {
      constructor.setAccessible(true);
      Void value = constructor.newInstance();

      Assert.assertNull(converter.toByteArray(value));
      Assert.assertNull(converter.fromByteArray(new byte[] {}, StandardCharsets.UTF_8));
      Assert.assertNull(converter.toString(value));
      Assert.assertNull(converter.fromString("void"));
    } finally {
      constructor.setAccessible(false);
    }
  }

  @Test public void unsupportedType() {
    try {
      ValueConverterService.def().getConverter(Object.class);
      Assert.fail("should throw an IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals("Unsupported ValueType <class java.lang.Object>", ex.getMessage());
    }
  }
  
  private <T> void convert(T value, ValueConverter<T> converter) throws ConverterException {
    byte[] buf    = converter.toByteArray(value);
    T      actual = converter.fromByteArray(buf, StandardCharsets.UTF_8);
    Assert.assertEquals(value, actual);

    String str = converter.toString(value);
    actual = converter.fromString(str);
    Assert.assertEquals(value, actual);
  }
}
