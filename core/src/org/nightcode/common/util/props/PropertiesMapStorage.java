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

import java.util.Map;

/**
 * PropertiesStorage Map-based implementation.
 */
public class PropertiesMapStorage implements PropertiesStorage {

  private final Map<String, Object> properties;

  public PropertiesMapStorage(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override public boolean readBoolean(String key) throws PropertyException {
    if (!properties.containsKey(key)) {
      throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
    }
    return (boolean) properties.get(key);
  }

  @Override public byte readByte(String key) throws PropertyException {
    if (!properties.containsKey(key)) {
      throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
    }
    return (byte) properties.get(key);
  }

  @Override public int readInt(String key) throws PropertyException {
    if (!properties.containsKey(key)) {
      throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
    }
    return (int) properties.get(key);
  }

  @Override public long readLong(String key) throws PropertyException {
    if (!properties.containsKey(key)) {
      throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
    }
    return (long) properties.get(key);
  }

  @Override public String readString(String key) throws PropertyException {
    if (!properties.containsKey(key)) {
      throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
    }
    return (String) properties.get(key);
  }
}
