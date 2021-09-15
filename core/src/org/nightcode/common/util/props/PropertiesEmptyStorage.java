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

final class PropertiesEmptyStorage implements PropertiesStorage {

  private static final PropertiesStorage INSTANCE = new PropertiesEmptyStorage();

  static PropertiesStorage instance() {
    return INSTANCE;
  }

  private PropertiesEmptyStorage() {
    // do nothing
  }

  @Override public Property readProperty(String key, Type type, NotFoundPolicy notFoundPolicy)
      throws PropertyException {
    return notFoundPolicy.apply(key, type);
  }
}
