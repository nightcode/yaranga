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

package org.nightcode.common.util.logging;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LoggingHandler interface.
 */
public interface LoggingHandler {

  boolean isEnabled(@NotNull Class<?> clazz);

  void log(@NotNull Class<?> clazz, String message);

  void log(@NotNull Class<?> clazz, String message, Object... params);

  void log(@NotNull Class<?> clazz, String message, @Nullable Throwable thrown);

  void log(@NotNull Class<?> clazz, Supplier<String> supplier, @Nullable Throwable thrown);

  void log(@NotNull Class<?> clazz, Throwable thrown, String message, Object... params);

  default void log(@NotNull Class<?> clazz, Throwable thrown) {
    log(clazz, "", thrown);
  }

  default void log(@NotNull Class<?> clazz, Supplier<String> supplier) {
    log(clazz, supplier, null);
  }
}
