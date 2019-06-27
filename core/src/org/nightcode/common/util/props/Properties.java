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
 * Main properties interface.
 */
public interface Properties {

  static Properties of(PropertiesStorage storage) {
    return new PropertiesImpl(storage);
  }

  boolean getBooleanValue(String key);

  boolean getBooleanValue(String key, boolean def);

  byte getByteValue(String key);

  byte getByteValue(String key, byte def);

  int getIntValue(String key);

  int getIntValue(String key, int def);

  long getLongValue(String key);

  long getLongValue(String key, long def);

  String getStringValue(String key);

  String getStringValue(String key, String def);
}
