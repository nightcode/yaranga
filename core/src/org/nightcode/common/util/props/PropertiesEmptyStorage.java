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

package org.nightcode.common.util.props;

import org.nightcode.common.util.props.PropertyException.ErrorCode;

final class PropertiesEmptyStorage implements PropertiesStorage {

  private static final PropertiesStorage INSTANCE = new PropertiesEmptyStorage();

  private static final PropertyException NOT_FOUND
      = new PropertyException(ErrorCode.PROPERTY_NOT_FOUND, "'empty' storage implementation");

  static PropertiesStorage instance() {
    return INSTANCE;
  }

  private PropertiesEmptyStorage() {
    // do nothing
  }

  @Override public boolean readBoolean(String key) throws PropertyException {
    throw NOT_FOUND;
  }

  @Override public byte readByte(String key) throws PropertyException {
    throw NOT_FOUND;
  }

  @Override public int readInt(String key) throws PropertyException {
    throw NOT_FOUND;
  }

  @Override public long readLong(String key) throws PropertyException {
    throw NOT_FOUND;
  }

  @Override public String readString(String key) throws PropertyException {
    throw NOT_FOUND;
  }
}
