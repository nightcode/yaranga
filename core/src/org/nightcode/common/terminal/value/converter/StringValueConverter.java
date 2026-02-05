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
import java.nio.charset.StandardCharsets;

/**
 * String value converter.
 */
public final class StringValueConverter extends AbstractValueConverter<String> {

  @Override protected String fromByteArray0(byte[] input, Charset encoding) {
    return new String(input, encoding);
  }

  @Override protected String fromString0(String input) {
    return input;
  }

  @Override protected byte[] toByteArray0(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }

  @Override protected String toString0(String input) {
    return input;
  }
}
