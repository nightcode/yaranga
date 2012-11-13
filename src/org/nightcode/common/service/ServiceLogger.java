/*
 * Copyright (C) 2008 The NightCode Open Source Project
 *
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

package org.nightcode.common.service;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.nightcode.common.base.Objects;

/**
 * Helps to simplify logging information using varargs.
 */
public final class ServiceLogger {

  /**
   * Returns an instance of {@link ServiceLogger}.
   *
   * @param name logger name
   * @return a suitable Logger
   */
  public static ServiceLogger getLogger(String name) {
    return new ServiceLogger(Logger.getLogger(name), name);
  }

  private final Logger logger;
  private final String name;

  /**
   * Creates a new SmartLogger by wrapping a {@code Logger}.
   *
   * @param logger the {@code Logger} to wrap
   * @param name a logger name
   */
  private ServiceLogger(Logger logger, String name) {
    Objects.nonNull(logger, "logger");
    this.logger = logger;
    this.name = name;
  }

  /**
   * Log a CONFIG message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void config(String message, Object... messageArgs) {
    log(Level.CONFIG, message, messageArgs);
  }

  /**
   * Log a CONFIG message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void config(String message, Throwable thrown, Object... messageArgs) {
    log(Level.CONFIG, message, thrown, messageArgs);
  }

  /**
   * Log a FINE message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void fine(String message, Object... messageArgs) {
    log(Level.FINE, message, messageArgs);
  }

  /**
   * Log a FINE message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void fine(String message, Throwable thrown, Object... messageArgs) {
    log(Level.FINE, message, thrown, messageArgs);
  }

  /**
   * Log a FINER message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void finer(String message, Object... messageArgs) {
    log(Level.FINER, message, messageArgs);
  }

  /**
   * Log a FINER message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void finer(String message, Throwable thrown, Object... messageArgs) {
    log(Level.FINER, message, thrown, messageArgs);
  }

  /**
   * Log a FINEST message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void finest(String message, Object... messageArgs) {
    log(Level.FINEST, message, messageArgs);
  }

  /**
   * Log a FINEST message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void finest(String message, Throwable thrown, Object... messageArgs) {
    log(Level.FINEST, message, thrown, messageArgs);
  }

  /**
   * Log an INFO message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void info(String message, Object... messageArgs) {
    log(Level.INFO, message, messageArgs);
  }

  /**
   * Log an INFO message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void info(String message, Throwable thrown, Object... messageArgs) {
    log(Level.INFO, message, thrown, messageArgs);
  }

  /**
   * Check if a message of the given level would actually be logged
   * by this logger.  This check is based on the Loggers effective level,
   * which may be inherited from its parent.
   *
   * @param level a message logging level
   * @return true if the given message level is currently being logged
   */
  public boolean isLoggable(Level level) {
    return logger.isLoggable(level);
  }

  /**
   * Log a message.
   * <p>
   * If the logger is currently enabled for the given message
   * level then a corresponding LogRecord is created and forwarded
   * to all the registered output Handler objects.
   * <p>
   *
   * @param level one of the message level identifiers, e.g. SEVERE
   * @param message the string message
   */
  public void log(Level level, String message) {
    logRecord(level, message, null);
  }

  /**
   * Log a message, with an array of object arguments.
   * <p>
   * If the logger is currently enabled for the given message
   * level then a corresponding LogRecord is created and forwarded
   * to all the registered output Handler objects.
   * <p>
   *
   * @param level one of the message level identifiers, e.g. SEVERE
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void log(Level level, String message, Object... messageArgs) {
    logRecord(level, format(message, messageArgs), null);
  }

  /**
   * Log a message, with associated Throwable information.
   * <p>
   * If the logger is currently enabled for the given message
   * level then the given arguments are stored in a LogRecord
   * which is forwarded to all registered output handlers.
   * <p>
   * Note that the thrown argument is stored in the LogRecord thrown
   * property, rather than the LogRecord parameters property.  Thus is it
   * processed specially by output Formatters and is not treated
   * as a formatting parameter to the LogRecord message property.
   * <p>
   *
   * @param level one of the message level identifiers, e.g. SEVERE
   * @param message the string message
   * @param thrown throwable associated with log message
   */
  public void log(Level level, String  message, Throwable thrown) {
    logRecord(level, message, thrown);
  }

  /**
   * Log a message, with an array of object arguments.
   * <p>
   * If the logger is currently enabled for the given message
   * level then a corresponding LogRecord is created and forwarded
   * to all the registered output Handler objects.
   * <p>
   *
   * @param level one of the message level identifiers, e.g. SEVERE
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void log(Level level, String message, Throwable thrown, Object... messageArgs) {
    logRecord(level, format(message, messageArgs), thrown);
  }

  /**
   * Log a SEVERE message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void severe(String message, Object... messageArgs) {
    log(Level.SEVERE, message, messageArgs);
  }

  /**
   * Log a SEVERE message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void severe(String message, Throwable thrown, Object... messageArgs) {
    log(Level.SEVERE, message, thrown, messageArgs);
  }

  /**
   * Log a WARNING message.
   *
   * @param message the string message
   * @param messageArgs array of parameters to the message
   */
  public void warning(String message, Object... messageArgs) {
    log(Level.WARNING, message, messageArgs);
  }

  /**
   * Log a WARNING message.
   *
   * @param message the string message
   * @param thrown throwable associated with log message
   * @param messageArgs array of parameters to the message
   */
  public void warning(String message, Throwable thrown, Object... messageArgs) {
    log(Level.WARNING, message, thrown, messageArgs);
  }

  /**
   * Returns a formatted string using the specified format string and arguments.
   *
   * @param message a format string
   * @param messageArgs arguments referenced by the format specifiers in the format string.
   *        If there are more arguments than format specifiers, the extra arguments are ignored.
   *        The number of arguments is variable and may be zero.
   * @return a formatted string
   */
  private String format(String message, Object... messageArgs) {
    StringBuilder sb = new StringBuilder(message.length() + 8 * messageArgs.length);
    int templateStart = 0;
    for (Object argument : messageArgs) {
      int placeholderStart = message.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      sb.append(message.substring(templateStart, placeholderStart));
      sb.append(argument);
      templateStart = placeholderStart + 2;
    }
    sb.append(message.substring(templateStart));
    return sb.toString();
  }

  /**
   * Log a LogRecord.
   * <p>
   * All the other logging methods in this class call through
   * this method to actually perform any logging.  Subclasses can
   * override this single method to capture all log activity.
   *
   * @param level one of the message level identifiers, e.g. SEVERE
   * @param message the string message
   * @param thrown throwable associated with log message
   */
  private void logRecord(Level level, String message, Throwable thrown) {
    if (!logger.isLoggable(level)) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(Thread.currentThread().getName());
    sb.append('(').append(Thread.currentThread().getId()).append(")/");
    sb.append(message);
    LogRecord record = new LogRecord(level, sb.toString());
    record.setThrown(thrown);
    record.setLoggerName(name);
    logger.log(record);
  }
}
