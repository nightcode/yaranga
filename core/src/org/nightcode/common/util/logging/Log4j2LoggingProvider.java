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

import org.apache.logging.log4j.LogManager;

/**
 * Provider for Log4j2.
 */
public final class Log4j2LoggingProvider implements LoggingProvider {

  private static final LoggingProvider INSTANCE = new Log4j2LoggingProvider();

  public static LoggingProvider instance() {
    return INSTANCE;
  }

  private Log4j2LoggingProvider() {
    // do nothing
  }

  @Override public Logger createLogger(Class<?> clazz) {
    return new Log4J2LoggerAdapter(LogManager.getFormatterLogger(clazz));
  }

  @Override public Logger createLogger(Object instance) {
    return new Log4J2LoggerAdapter(LogManager.getFormatterLogger(instance));
  }
}
