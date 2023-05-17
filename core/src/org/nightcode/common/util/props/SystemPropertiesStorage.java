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
public enum SystemPropertiesStorage implements PropertiesStorage {

  INSTANCE;

  @Override public Property readProperty(String key, Type type, NotFoundPolicy notFoundPolicy)
      throws PropertyException {
    String value = readPropertyValue(key);
    if (value == null) {
      return notFoundPolicy.apply(key, type);
    }
    Property property;
    switch (type) {
      case BOOLEAN:
        property = Property.createBoolean(Boolean.parseBoolean(value));
        break;
      case BYTE:
        property = Property.createByte(Byte.parseByte(value));
        break;
      case INT:
        property = Property.createInt(Integer.parseInt(value));
        break;
      case LONG:
        property = Property.createLong(Long.parseLong(value));
        break;
      case STRING:
        property = Property.createString(readPropertyValue(key));
        break;
      default:
        throw new PropertyException("unsupported property type: " + type);
    }
    return property;
  }

  private String readPropertyValue(String key) {
    String value = System.getProperty(key);
    if (value == null) {
      value = System.getenv(key);
    }
    return value;
  }
}
