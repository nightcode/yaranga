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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.nightcode.common.terminal.value.converter.BigDecimalValueConverter;
import org.nightcode.common.terminal.value.converter.BooleanValueConverter;
import org.nightcode.common.terminal.value.converter.ByteValueConverter;
import org.nightcode.common.terminal.value.converter.DoubleValueConverter;
import org.nightcode.common.terminal.value.converter.FloatValueConverter;
import org.nightcode.common.terminal.value.converter.IntegerValueConverter;
import org.nightcode.common.terminal.value.converter.LongValueConverter;
import org.nightcode.common.terminal.value.converter.ShortValueConverter;
import org.nightcode.common.terminal.value.converter.StringValueConverter;
import org.nightcode.common.terminal.value.converter.UuidValueConverter;
import org.nightcode.common.terminal.value.converter.VoidValueConverter;

/**
 * Simple implementation of ValueConverterService.
 */
public final class ValueConverterServiceImpl implements ValueConverterService {

  private static final Map<Class<?>, ValueConverter<?>> SIMPLE_PARAMETER_CONVERTERS;

  static {
    Map<Class<?>, ValueConverter<?>> builder = new HashMap<>();
    builder.put(boolean.class,    new BooleanValueConverter());
    builder.put(Boolean.class,    new BooleanValueConverter());
    builder.put(byte.class,       new ByteValueConverter());
    builder.put(Byte.class,       new ByteValueConverter());
    builder.put(short.class,      new ShortValueConverter());
    builder.put(Short.class,      new ShortValueConverter());
    builder.put(int.class,        new IntegerValueConverter());
    builder.put(Integer.class,    new IntegerValueConverter());
    builder.put(long.class,       new LongValueConverter());
    builder.put(Long.class,       new LongValueConverter());
    builder.put(float.class,      new FloatValueConverter());
    builder.put(Float.class,      new FloatValueConverter());
    builder.put(double.class,     new DoubleValueConverter());
    builder.put(Double.class,     new DoubleValueConverter());
    builder.put(BigDecimal.class, new BigDecimalValueConverter());
    builder.put(String.class,     new StringValueConverter());
    builder.put(UUID.class,       new UuidValueConverter());
    builder.put(void.class,       new VoidValueConverter());
    builder.put(Void.class,       new VoidValueConverter());
    SIMPLE_PARAMETER_CONVERTERS = Collections.unmodifiableMap(builder);
  }

  private static final ValueConverterService INSTANCE = new ValueConverterServiceImpl();

  public static ValueConverterService instance() {
    return INSTANCE;
  }

  private ValueConverterServiceImpl() {
    // do nothing
  }

  @Override public ValueConverter<?> getConverter(Type valueType) {
    ValueConverter<?> converter = SIMPLE_PARAMETER_CONVERTERS.get((Class<?>) valueType);
    if (converter == null) {
      throw new IllegalArgumentException("Unsupported ValueType <" + valueType + ">");
    }
    return converter;
  }
}
