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

package org.nightcode.common.config;

import java.io.InputStream;
import java.io.Reader;

import org.nightcode.common.lang.ThrowingSupplier;

/**
 * Config loader interface.
 */
public interface ConfigLoader {

  String CONFIG_USE_ENV = "org.nightcode.config.useEnv";

  static ConfigLoader yaml() {
    return YamlConfigLoader.INSTANCE;
  }

  <T> T loadConfig(Class<T> clazz, InputStream src);

  <T> T loadConfig(Class<T> clazz, Reader src);

  <T> T loadConfig(Class<T> clazz, String yaml);

  <T> T loadConfig(Class<T> clazz, ThrowingSupplier<Reader, Exception> supplier);
}
