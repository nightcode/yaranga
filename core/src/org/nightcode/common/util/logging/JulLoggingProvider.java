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

/**
 * Provider for JUL.
 * @deprecated would be removed in next release
 */
@Deprecated
public final class JulLoggingProvider implements LoggingProvider {

  private static final LoggingProvider INSTANCE = new JulLoggingProvider();

  public static LoggingProvider instance() {
    return INSTANCE;
  }

  private JulLoggingProvider() {
    // do nothing
  }

  @Override public Logger createLogger(Class<?> clazz) {
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(clazz.getName());
    return new JulLoggerAdapter(clazz.getName(), logger);
  }

  @Override public Logger createLogger(Object instance) {
    String className = (instance != null) ? instance.getClass().getName() : "[NULL]";
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(className);
    return new JulLoggerAdapter(className, logger);
  }

  @Override public Logger createLogger(String name) {
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
    return new JulLoggerAdapter(name, logger);
  }
}
