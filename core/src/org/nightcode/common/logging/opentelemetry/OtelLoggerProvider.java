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

package org.nightcode.common.logging.opentelemetry;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import org.jetbrains.annotations.Nullable;
import org.nightcode.common.props.Properties;

/**
 * LoggerProvider holder.
 */
public enum OtelLoggerProvider {
  ;

  private static volatile LoggerProvider loggerProvider;

  private static Throwable lastCaller;

  private static final Object MUTEX = new Object();

  public static void init(LoggerProvider provider) {
    synchronized (MUTEX) {
      if (loggerProvider != null) {
        throw new IllegalStateException("OtelLoggerProvider has already been initialized.", lastCaller);
      }
      if (Properties.instance().getBoolean("org.nightcode.logging.opentelemetry.disable", false)) {
        provider = LoggerProvider.noop();
      }
      loggerProvider = provider;
      lastCaller     = new Throwable();
    }
  }

  public static Logger get(Class<?> clazz) {
    return instance().get(clazz.getName());
  }

  public static LoggerProvider instance() {
    LoggerProvider provider = loggerProvider;
    if (provider == null) {
      synchronized (MUTEX) {
        provider = loggerProvider;
        if (provider == null) {
          init(LoggerProvider.noop());
          provider = loggerProvider;
        }
      }
    }
    return provider;
  }

  public static @Nullable SdkLoggerProvider sdkInstance() {
    if (instance() instanceof SdkLoggerProvider sdkLoggerProvider) {
      return sdkLoggerProvider;
    }
    return null;
  }

  public static boolean isNoop() {
    return instance() == LoggerProvider.noop();
  }
}
