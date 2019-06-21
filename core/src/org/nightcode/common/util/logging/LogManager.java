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
 * Logging manager.
 */
public final class LogManager {

  public static Logger getLogger(Class<?> clazz) {
    return INSTANCE.getLoggerImpl(clazz);
  }

  public static Logger getLogger(Object instance) {
    return INSTANCE.getLoggerImpl(instance);
  }

  public static LogManager instance() {
    return INSTANCE;
  }

  private static final LogManager INSTANCE = new LogManager();

  private volatile LoggingProvider provider = JulLoggingProvider.instance();

  private LogManager() {
    // do nothing
  }

  public synchronized void setLoggingProvider(LoggingProvider provider) {
    this.provider = provider;
  }

  private Logger getLoggerImpl(Class<?> clazz) {
    return provider.createLogger(clazz);
  }

  private Logger getLoggerImpl(Object instance) {
    return provider.createLogger(instance);
  }
}
