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

/**
 * PropertiesStorage implementation which tries to read value from the system property
 * and if the system property is not defined tries to read from the environment variable.
 */
public class SystemPropertiesStorage implements PropertiesStorage {

  @Override public boolean readBoolean(String key) throws PropertyException {
    String value = readPropertyValue(key);
    return Boolean.parseBoolean(value);
  }

  @Override public byte readByte(String key) throws PropertyException {
    String value = readPropertyValue(key);
    return Byte.parseByte(value);
  }

  @Override public int readInt(String key) throws PropertyException {
    String value = readPropertyValue(key);
    return Integer.parseInt(value);
  }

  @Override public long readLong(String key) throws PropertyException {
    String value = readPropertyValue(key);
    return Long.parseLong(value);
  }

  @Override public String readString(String key) throws PropertyException {
    return readPropertyValue(key);
  }

  private String readPropertyValue(String key) throws PropertyException {
    String value = System.getProperty(key);

    if (value == null) {
      value = System.getenv(key);
    }

    if (value == null) {
      throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
    }

    return value;
  }
}
