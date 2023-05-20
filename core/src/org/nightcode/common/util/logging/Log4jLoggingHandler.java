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

import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Log4jLoggingHandler implements LoggingHandler {

  TRACE(Level.TRACE),
  DEBUG(Level.DEBUG),
  INFO(Level.INFO),
  WARN(Level.WARN),
  ERROR(Level.ERROR),
  FATAL(Level.FATAL);

  static org.apache.logging.log4j.Logger getLogger(Class<?> clazz) {
    return CLASS_LOGGER.get(clazz);
  }

  static final ClassValue<org.apache.logging.log4j.Logger> CLASS_LOGGER = new ClassValue<org.apache.logging.log4j.Logger>() {
    @Override protected org.apache.logging.log4j.Logger computeValue(Class<?> type) {
      return org.apache.logging.log4j.LogManager.getLogger(type.getName());
    }
  };

  private final Level level;

  Log4jLoggingHandler(Level level) {
    this.level = level;
  }

  public boolean isEnabled(@NotNull Class<?> clazz) {
    return getLogger(clazz).isEnabled(level);
  }

  @Override public void log(@NotNull Class<?> clazz, String message) {
    getLogger(clazz).log(level, message);
  }

  @Override public void log(@NotNull Class<?> clazz, String message, Object... params) {
    getLogger(clazz).log(level, message, params);
  }

  @Override public void log(@NotNull Class<?> clazz, String message, @Nullable Throwable thrown) {
    getLogger(clazz).log(level, message, thrown);
  }

  @Override public void log(@NotNull Class<?> clazz, Supplier<String> supplier, @Nullable Throwable thrown) {
    getLogger(clazz).log(level, (org.apache.logging.log4j.util.Supplier<?>) supplier::get, thrown);
  }

  @Override public void log(@NotNull Class<?> clazz, Throwable throwable, String message, Object... params) {
    getLogger(clazz).atLevel(level).withThrowable(throwable).log(message, params);
  }
}
