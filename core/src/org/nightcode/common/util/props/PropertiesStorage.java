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

import org.nightcode.common.annotations.Beta;

/**
 * Main Storage interface.
 */
@Beta
public interface PropertiesStorage {

  /**
   * Property Type enum.
   */
  enum Type {
    BOOLEAN,
    BYTE,
    INT,
    LONG,
    STRING
  }

  /**
   * NotFound policy interface.
   */
  interface NotFoundPolicy {
    Property apply(String key, Type type) throws PropertyException;
  }

  NotFoundPolicy NULL_POLICY = (key, type) -> null;

  NotFoundPolicy THROW = (key, type) -> {
    throw new PropertyNotFoundException("unable to read property '" + key + "' of type " + type);
  };

  Property readProperty(String key, Type type, NotFoundPolicy notFoundPolicy) throws PropertyException;

  default Property readProperty(String key, Type type) throws PropertyException {
    return readProperty(key, type, THROW);
  }
}
