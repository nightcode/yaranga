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

  @Override public Property readProperty(String key, Type type, NotFoundPolicy notFoundPolicy)
      throws PropertyException {
    if (!properties.containsKey(key)) {
      return notFoundPolicy.apply(key, type);
    }
    Property property;
    switch (type) {
      case BOOLEAN:
        property = Property.createBoolean((boolean) properties.get(key));
        break;
      case BYTE:
        property = Property.createByte((byte) properties.get(key));
        break;
      case INT:
        property = Property.createInt((int) properties.get(key));
        break;
      case LONG:
        property = Property.createLong((long) properties.get(key));
        break;
      case STRING:
        property = Property.createString((String) properties.get(key));
        break;
      default:
        throw new PropertyException("unsupported property type: " + type);
    }
    return property;
  }
}
