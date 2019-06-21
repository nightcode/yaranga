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

import org.apache.logging.log4j.Level;

class Log4J2LoggerAdapter implements Logger {

  private static org.apache.logging.log4j.util.Supplier<?> toLog4jSupplier(Supplier<?> target) {
    return target::get;
  }

  private static org.apache.logging.log4j.util.Supplier<?>[] toLog4jSuppliers(Supplier<?>... args) {
    return (org.apache.logging.log4j.util.Supplier<?>[]) Arrays.stream(args)
        .map(Log4J2LoggerAdapter::toLog4jSupplier).toArray();
  }

  private final org.apache.logging.log4j.Logger logger;

  Log4J2LoggerAdapter(org.apache.logging.log4j.Logger logger) {
    this.logger = logger;
  }

  @Override public void trace(String message) {
    log(Level.TRACE, message);
  }

  @Override public void trace(String message, Object... args) {
    log(Level.TRACE, message, args);
  }

  @Override public void trace(Supplier<?> supplier) {
    log(Level.TRACE, supplier);
  }

  @Override public void trace(String message, Supplier<?>... args) {
    log(Level.TRACE, message, args);
  }

  @Override public void trace(String message, Throwable thrown) {
    log(Level.TRACE, message, thrown);
  }

  @Override public void trace(Throwable thrown, Supplier<?> supplier) {
    log(Level.TRACE, thrown, supplier);
  }

  @Override public void debug(String message) {
    log(Level.DEBUG, message);
  }

  @Override public void debug(String message, Object... args) {
    log(Level.DEBUG, message, args);
  }

  @Override public void debug(Supplier<?> supplier) {
    log(Level.DEBUG, supplier);
  }

  @Override public void debug(String message, Supplier<?>... args) {
    log(Level.DEBUG, message, args);
  }

  @Override public void debug(String message, Throwable thrown) {
    log(Level.DEBUG, message, thrown);
  }

  @Override public void debug(Throwable thrown, Supplier<?> supplier) {
    log(Level.DEBUG, thrown, supplier);
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
    log(Level.INFO, message);
  }

  @Override public void config(String message, Object... args) {
    log(Level.INFO, message, args);
  }

  @Override public void config(Supplier<?> supplier) {
    log(Level.INFO, supplier);
  }

  @Override public void config(String message, Supplier<?>... args) {
    log(Level.INFO, message, args);
  }

  @Override public void config(String message, Throwable thrown) {
    log(Level.INFO, message, thrown);
  }

  @Override public void config(Throwable thrown, Supplier<?> supplier) {
    log(Level.INFO, thrown, supplier);
  }

  @Override public void warn(String message) {
    log(Level.WARN, message);
  }

  @Override public void warn(String message, Object... args) {
    log(Level.WARN, message, args);
  }

  @Override public void warn(Supplier<?> supplier) {
    log(Level.WARN, supplier);
  }

  @Override public void warn(String message, Supplier<?>... args) {
    log(Level.WARN, message, args);
  }

  @Override public void warn(String message, Throwable thrown) {
    log(Level.WARN, message, thrown);
  }

  @Override public void warn(Throwable thrown, Supplier<?> supplier) {
    log(Level.WARN, thrown, supplier);
  }

  @Override public void error(String message) {
    log(Level.ERROR, message);
  }

  @Override public void error(String message, Object... args) {
    log(Level.ERROR, message, args);
  }

  @Override public void error(Supplier<?> supplier) {
    log(Level.ERROR, supplier);
  }

  @Override public void error(String message, Supplier<?>... args) {
    log(Level.ERROR, message, args);
  }

  @Override public void error(String message, Throwable thrown) {
    log(Level.ERROR, message, thrown);
  }

  @Override public void error(Throwable thrown, Supplier<?> supplier) {
    log(Level.ERROR, thrown, supplier);
  }

  @Override public void fatal(String message) {
    log(Level.FATAL, message);
  }

  @Override public void fatal(String message, Object... args) {
    log(Level.FATAL, message, args);
  }

  @Override public void fatal(Supplier<?> supplier) {
    log(Level.FATAL, supplier);
  }

  @Override public void fatal(String message, Supplier<?>... args) {
    log(Level.FATAL, message, args);
  }

  @Override public void fatal(String message, Throwable thrown) {
    log(Level.FATAL, message, thrown);
  }

  @Override public void fatal(Throwable thrown, Supplier<?> supplier) {
    log(Level.FATAL, thrown, supplier);
  }
  
  private void log(Level level, String message) {
    logger.log(level, message);
  }

  private void log(Level level, String message, Object... args) {
    logger.log(level, message, args);
  }

  private void log(Level level, Supplier<?> supplier) {
    logger.log(level, toLog4jSupplier(supplier));
  }

  private void log(Level level, String message, Supplier<?>... args) {
    logger.log(level, message, toLog4jSuppliers(args));
  }

  private void log(Level level, String message, Throwable thrown) {
    logger.log(level, message, thrown);
  }

  private void log(Level level, Throwable thrown, Supplier<?> supplier) {
    logger.log(level, toLog4jSupplier(supplier), thrown);
  }
}
