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

import java.nio.charset.Charset;

import org.jetbrains.annotations.Nullable;
import org.nightcode.common.terminal.value.converter.ConverterException;

/**
 * Base value converter interface.
 *
 * @param <T> the type
 */
public interface ValueConverter<T> {

  T fromByteArray(@Nullable byte[] input, Charset encoding) throws ConverterException;

  T fromString(@Nullable String input) throws ConverterException;

  byte[] toByteArray(@Nullable T input);

  String toString(@Nullable T input);
}
