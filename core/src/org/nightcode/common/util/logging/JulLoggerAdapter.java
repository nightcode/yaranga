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
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;

class JulLoggerAdapter implements Logger {

  private static Supplier<String> toStringSupplier(Supplier<?> target) {
    return () -> {
      Object obj = target.get();
      return (obj != null) ? obj.toString() : null;
    };
  }

  private final String sourceClassName;
  private final java.util.logging.Logger logger;

  JulLoggerAdapter(String sourceClassName, java.util.logging.Logger logger) {
    this.sourceClassName = sourceClassName;
    this.logger = logger;
  }

  @Override public void trace(String message) {
    log(Level.FINEST, message);
  }

  @Override public void trace(String message, Object... args) {
    log(Level.FINEST, message, args);
  }

  @Override public void trace(String message, Throwable thrown) {
    log(Level.FINEST, message, thrown);
  }

  @Override public void trace(Throwable thrown, String message, Object... args) {
    log(Level.FINEST, () -> String.format(message, args), thrown);
  }

  @Override public void trace(Supplier<?> supplier) {
    log(Level.FINEST, supplier);
  }

  @Override public void trace(Supplier<?> supplier, Throwable thrown) {
    log(Level.FINEST, supplier, thrown);
  }

  @Override public void debug(String message) {
    log(Level.FINE, message);
  }

  @Override public void debug(String message, Object... args) {
    log(Level.FINE, message, args);
  }

  @Override public void debug(String message, Throwable thrown) {
    log(Level.FINE, message, thrown);
  }

  @Override public void debug(Throwable thrown, String message, Object... args) {
    log(Level.FINE, () -> String.format(message, args), thrown);
  }

  @Override public void debug(Supplier<?> supplier) {
    log(Level.FINE, supplier);
  }

  @Override public void debug(Supplier<?> supplier, Throwable thrown) {
    log(Level.FINE, supplier, thrown);
  }

  @Override public void info(String message) {
    log(Level.INFO, message);
  }

  @Override public void info(String message, Object... args) {
    log(Level.INFO, message, args);
  }

  @Override public void info(String message, Throwable thrown) {
    log(Level.INFO, message, thrown);
  }

  @Override public void info(Throwable thrown, String message, Object... args) {
    log(Level.INFO, () -> String.format(message, args), thrown);
  }

  @Override public void info(Supplier<?> supplier) {
    log(Level.INFO, supplier);
  }

  @Override public void info(Supplier<?> supplier, Throwable thrown) {
    log(Level.INFO, supplier, thrown);
  }

  @Override public void config(String message) {
    log(Level.CONFIG, message);
  }

  @Override public void config(String message, Object... args) {
    log(Level.CONFIG, message, args);
  }

  @Override public void config(String message, Throwable thrown) {
    log(Level.CONFIG, message, thrown);
  }

  @Override public void config(Throwable thrown, String message, Object... args) {
    log(Level.CONFIG, () -> String.format(message, args), thrown);
  }

  @Override public void config(Supplier<?> supplier) {
    log(Level.CONFIG, supplier);
  }

  @Override public void config(Supplier<?> supplier, Throwable thrown) {
    log(Level.CONFIG, supplier, thrown);
  }

  @Override public void warn(String message) {
    log(Level.WARNING, message);
  }

  @Override public void warn(String message, Object... args) {
    log(Level.WARNING, message, args);
  }

  @Override public void warn(String message, Throwable thrown) {
    log(Level.WARNING, message, thrown);
  }

  @Override public void warn(Throwable thrown, String message, Object... args) {
    log(Level.WARNING, () -> String.format(message, args), thrown);
  }

  @Override public void warn(Supplier<?> supplier) {
    log(Level.WARNING, supplier);
  }

  @Override public void warn(Supplier<?> supplier, Throwable thrown) {
    log(Level.WARNING, supplier, thrown);
  }

  @Override public void error(String message) {
    log(Level.WARNING, message);
  }

  @Override public void error(String message, Object... args) {
    log(Level.WARNING, message, args);
  }

  @Override public void error(String message, Throwable thrown) {
    log(Level.WARNING, message, thrown);
  }

  @Override public void error(Throwable thrown, String message, Object... args) {
    log(Level.WARNING, () -> String.format(message, args), thrown);
  }

  @Override public void error(Supplier<?> supplier) {
    log(Level.WARNING, supplier);
  }

  @Override public void error(Supplier<?> supplier, Throwable thrown) {
    log(Level.WARNING, supplier, thrown);
  }

  @Override public void fatal(String message) {
    log(Level.SEVERE, message);
  }

  @Override public void fatal(String message, Object... args) {
    log(Level.SEVERE, message, args);
  }

  @Override public void fatal(String message, Throwable thrown) {
    log(Level.SEVERE, message, thrown);
  }

  @Override public void fatal(Throwable thrown, String message, Object... args) {
    log(Level.SEVERE, () -> String.format(message, args), thrown);
  }

  @Override public void fatal(Supplier<?> supplier) {
    log(Level.SEVERE, supplier);
  }

  @Override public void fatal(Supplier<?> supplier, Throwable thrown) {
    log(Level.SEVERE, supplier, thrown);
  }

  private void log(Level level, String message) {
    logRecord(level, message, null);
  }

  private void log(Level level, String message, Object... args) {
    logRecord(level, null, () -> String.format(message, args));
  }

  private void log(Level level, String message, Throwable thrown) {
    logRecord(level, message, thrown);
  }

  private void log(Level level, Supplier<?> supplier) {
    logRecord(level, null, toStringSupplier(supplier));
  }

  private void log(Level level, Supplier<?> supplier, Throwable thrown) {
    logRecord(level, thrown, toStringSupplier(supplier));
  }

  private void logRecord(Level level, String message, @Nullable Throwable thrown) {
    if (!logger.isLoggable(level)) {
      return;
    }
    logRecord0(level, message, thrown);
  }

  private void logRecord(Level level, @Nullable Throwable thrown, Supplier<String> supplier) {
    if (!logger.isLoggable(level)) {
      return;
    }
    logRecord0(level, supplier.get(), thrown);
  }

  private void logRecord0(Level level, String message, @Nullable Throwable thrown) {
    LogRecord record = new LogRecord(level, message);
    record.setThrown(thrown);
    record.setSourceClassName(sourceClassName);
    logger.log(record);
  }
}
