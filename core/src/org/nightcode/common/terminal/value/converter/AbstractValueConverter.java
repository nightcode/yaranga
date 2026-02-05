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

import org.jetbrains.annotations.Nullable;
import org.nightcode.common.terminal.value.ValueConverter;

/**
 * Abstract value convertor.
 *
 * @param <T> the type
 */
public abstract class AbstractValueConverter<T> implements ValueConverter<T> {

  @Override public final T fromByteArray(@Nullable byte[] input, Charset encoding) throws ConverterException {
    if (input == null) {
      return null;
    }
    try {
      return fromByteArray0(input, encoding);
    } catch (Exception ex) {
      throw new ConverterException(ex.getMessage(), ex);
    }
  }

  @Override public final T fromString(@Nullable String input) throws ConverterException {
    if (input == null) {
      return null;
    }
    try {
      return fromString0(input);
    } catch (Exception ex) {
      throw new ConverterException(ex.getMessage());
    }
  }

  @Override public final byte[] toByteArray(@Nullable T input) {
    if (input == null) {
      return null;
    }
    return toByteArray0(input);
  }

  @Override public final String toString(@Nullable T input) {
    if (input == null) {
      return null;
    }
    return toString0(input);
  }

  @Override public String toString() {
    return this.getClass().getSimpleName();
  }

  protected abstract T fromByteArray0(byte[] input, Charset encoding) throws ConverterException;

  protected abstract T fromString0(String input);

  protected abstract byte[] toByteArray0(T input);

  protected String toString0(T input) {
    return String.valueOf(input);
  }
}
