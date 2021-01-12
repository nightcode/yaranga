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

/**
 * Main logger interface.
 */
public interface Logger {

  void log(LogLevel level, String message);

  void log(LogLevel level, String message, Object... args);

  void log(LogLevel level, String message, Throwable thrown);

  void log(LogLevel level, Throwable thrown, String message, Object... args);

  void log(LogLevel level, Supplier<?> supplier);

  void log(LogLevel level, Supplier<?> supplier, Throwable thrown);

  void trace(String message);

  void trace(String message, Object... args);

  void trace(String message, Throwable thrown);

  void trace(Throwable thrown, String message, Object... args);

  void trace(Supplier<?> supplier);

  void trace(Supplier<?> supplier, Throwable thrown);

  void debug(String message);

  void debug(String message, Object... args);

  void debug(String message, Throwable thrown);

  void debug(Throwable thrown, String message, Object... args);

  void debug(Supplier<?> supplier);

  void debug(Supplier<?> supplier, Throwable thrown);

  void info(String message);

  void info(String message, Object... args);

  void info(String message, Throwable thrown);

  void info(Throwable thrown, String message, Object... args);

  void info(Supplier<?> supplier);

  void info(Supplier<?> supplier, Throwable thrown);

  boolean isTraceLoggable();

  boolean isDebugLoggable();

  boolean isInfoLoggable();

  boolean isConfigLoggable();

  boolean isWarnLoggable();

  boolean isErrorLoggable();

  boolean isFatalLoggable();

  void config(String message);

  void config(String message, Object... args);

  void config(String message, Throwable thrown);

  void config(Throwable thrown, String message, Object... args);

  void config(Supplier<?> supplier);

  void config(Supplier<?> supplier, Throwable thrown);

  void warn(String message);

  void warn(String message, Object... args);

  void warn(String message, Throwable thrown);

  void warn(Throwable thrown, String message, Object... args);

  void warn(Supplier<?> supplier);

  void warn(Supplier<?> supplier, Throwable thrown);

  void error(String message);

  void error(String message, Object... args);

  void error(String message, Throwable thrown);

  void error(Throwable thrown, String message, Object... args);

  void error(Supplier<?> supplier);

  void error(Supplier<?> supplier, Throwable thrown);

  void fatal(String message);

  void fatal(String message, Object... args);

  void fatal(String message, Throwable thrown);

  void fatal(Throwable thrown, String message, Object... args);

  void fatal(Supplier<?> supplier);

  void fatal(Supplier<?> supplier, Throwable thrown);
}
