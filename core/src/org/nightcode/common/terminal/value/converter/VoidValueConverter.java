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

/**
 * Void value converter.
 */
public class VoidValueConverter extends AbstractValueConverter<Void> {

  @Override protected Void fromByteArray0(byte[] input, Charset encoding) {
    return null;
  }

  @Override protected Void fromString0(String input) {
    return null;
  }

  @Override protected byte[] toByteArray0(Void input) {
    return null;
  }

  @Override protected String toString0(Void input) {
    return null;
  }
}
