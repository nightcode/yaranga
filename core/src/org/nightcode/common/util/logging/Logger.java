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

  void trace(String message);

  void trace(String message, Object... args);

  void trace(Supplier<?> supplier);

  void trace(String message, Supplier<?>... args);

  void trace(String message, Throwable thrown);

  void trace(Throwable thrown, Supplier<?> supplier);

  void debug(String message);

  void debug(String message, Object... args);

  void debug(Supplier<?> supplier);

  void debug(String message, Supplier<?>... args);

  void debug(String message, Throwable thrown);

  void debug(Throwable thrown, Supplier<?> supplier);

  void info(String message);

  void info(String message, Object... args);

  void info(Supplier<?> supplier);

  void info(String message, Supplier<?>... args);

  void info(String message, Throwable thrown);

  void info(Throwable thrown, Supplier<?> supplier);

  void config(String message);

  void config(String message, Object... args);

  void config(Supplier<?> supplier);

  void config(String message, Supplier<?>... args);

  void config(String message, Throwable thrown);

  void config(Throwable thrown, Supplier<?> supplier);

  void warn(String message);

  void warn(String message, Object... args);

  void warn(Supplier<?> supplier);

  void warn(String message, Supplier<?>... args);

  void warn(String message, Throwable thrown);

  void warn(Throwable thrown, Supplier<?> supplier);

  void error(String message);

  void error(String message, Object... args);

  void error(Supplier<?> supplier);

  void error(String message, Supplier<?>... args);

  void error(String message, Throwable thrown);

  void error(Throwable thrown, Supplier<?> supplier);

  void fatal(String message);

  void fatal(String message, Object... args);

  void fatal(Supplier<?> supplier);

  void fatal(String message, Supplier<?>... args);

  void fatal(String message, Throwable thrown);

  void fatal(Throwable thrown, Supplier<?> arg);
}
