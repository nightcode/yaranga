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

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.logging.Level;

class JulLoggerAdapter implements Logger {

  private static Object[] suppliersToArray(Supplier<?>... args) {
    return Arrays.stream(args).map(Supplier::get).toArray();
  }

  private static Supplier<String> toStringSupplier(Supplier<?> target) {
    return () -> {
      Object obj = target.get();
      return (obj != null) ? obj.toString() : null;
    };
  }

  private final java.util.logging.Logger logger;

  JulLoggerAdapter(java.util.logging.Logger logger) {
    this.logger = logger;
  }

  @Override public void trace(String message) {
    log(Level.FINEST, message);
  }

  @Override public void trace(String message, Object... args) {
    log(Level.FINEST, message, args);
  }

  @Override public void trace(Supplier<?> supplier) {
    log(Level.FINEST, supplier);
  }

  @Override public void trace(String message, Supplier<?>... args) {
    log(Level.FINEST, message, args);
  }

  @Override public void trace(String message, Throwable thrown) {
    log(Level.FINEST, message, thrown);
  }

  @Override public void trace(Throwable thrown, Supplier<?> supplier) {
    log(Level.FINEST, thrown, supplier);
  }

  @Override public void debug(String message) {
    log(Level.FINE, message);
  }

  @Override public void debug(String message, Object... args) {
    log(Level.FINE, message, args);
  }

  @Override public void debug(Supplier<?> supplier) {
    log(Level.FINE, supplier);
  }

  @Override public void debug(String message, Supplier<?>... args) {
    log(Level.FINE, message, args);
  }

  @Override public void debug(String message, Throwable thrown) {
    log(Level.FINE, message, thrown);
  }

  @Override public void debug(Throwable thrown, Supplier<?> supplier) {
    log(Level.FINE, thrown, supplier);
  }

  @Override public void info(String message) {
    log(Level.INFO, message);
  }

  @Override public void info(String message, Object... args) {
    log(Level.INFO, message, args);
  }

  @Override public void info(Supplier<?> supplier) {
    log(Level.INFO, supplier);
  }

  @Override public void info(String message, Supplier<?>... args) {
    log(Level.INFO, message, args);
  }

  @Override public void info(String message, Throwable thrown) {
    log(Level.INFO, message, thrown);
  }

  @Override public void info(Throwable thrown, Supplier<?> supplier) {
    log(Level.INFO, thrown, supplier);
  }

  @Override public void config(String message) {
    log(Level.CONFIG, message);
  }

  @Override public void config(String message, Object... args) {
    log(Level.CONFIG, message, args);
  }

  @Override public void config(Supplier<?> supplier) {
    log(Level.CONFIG, supplier);
  }

  @Override public void config(String message, Supplier<?>... args) {
    log(Level.CONFIG, message, args);
  }

  @Override public void config(String message, Throwable thrown) {
    log(Level.CONFIG, message, thrown);
  }

  @Override public void config(Throwable thrown, Supplier<?> supplier) {
    log(Level.CONFIG, thrown, supplier);
  }

  @Override public void warn(String message) {
    log(Level.WARNING, message);
  }

  @Override public void warn(String message, Object... args) {
    log(Level.WARNING, message, args);
  }

  @Override public void warn(Supplier<?> supplier) {
    log(Level.WARNING, supplier);
  }

  @Override public void warn(String message, Supplier<?>... args) {
    log(Level.WARNING, message, args);
  }

  @Override public void warn(String message, Throwable thrown) {
    log(Level.WARNING, message, thrown);
  }

  @Override public void warn(Throwable thrown, Supplier<?> supplier) {
    log(Level.WARNING, thrown, supplier);
  }

  @Override public void error(String message) {
    log(Level.WARNING, message);
  }

  @Override public void error(String message, Object... args) {
    log(Level.WARNING, message, args);
  }

  @Override public void error(Supplier<?> supplier) {
    log(Level.WARNING, supplier);
  }

  @Override public void error(String message, Supplier<?>... args) {
    log(Level.WARNING, message, args);
  }

  @Override public void error(String message, Throwable thrown) {
    log(Level.WARNING, message, thrown);
  }

  @Override public void error(Throwable thrown, Supplier<?> supplier) {
    log(Level.WARNING, thrown, supplier);
  }

  @Override public void fatal(String message) {
    log(Level.SEVERE, message);
  }

  @Override public void fatal(String message, Object... args) {
    log(Level.SEVERE, message, args);
  }

  @Override public void fatal(Supplier<?> supplier) {
    log(Level.SEVERE, supplier);
  }

  @Override public void fatal(String message, Supplier<?>... args) {
    log(Level.SEVERE, message, args);
  }

  @Override public void fatal(String message, Throwable thrown) {
    log(Level.SEVERE, message, thrown);
  }

  @Override public void fatal(Throwable thrown, Supplier<?> supplier) {
    log(Level.SEVERE, thrown, supplier);
  }

  private void log(Level level, String message) {
    logger.log(level, message);
  }

  private void log(Level level, String message, Object... args) {
    logger.log(level, String.format(message, args));
  }

  private void log(Level level, Supplier<?> supplier) {
    logger.log(level, toStringSupplier(supplier));
  }

  private void log(Level level, String message, Supplier<?>... suppliers) {
    logger.log(level, message, suppliersToArray(suppliers));
  }

  private void log(Level level, String message, Throwable thrown) {
    logger.log(level, message, thrown);
  }

  private void log(Level level, Throwable thrown, Supplier<?> supplier) {
    logger.log(level, thrown, toStringSupplier(supplier));
  }
}
