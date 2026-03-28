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

package org.nightcode.common.trace.opentelemetry;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.jetbrains.annotations.Nullable;
import org.nightcode.common.props.Properties;

/**
 * TracerProvider holder.
 */
public enum OtelTracerProvider {
  ;

  private static volatile TracerProvider tracerProvider;

  private static Throwable lastCaller;

  private static final Object MUTEX = new Object();

  public static void init(TracerProvider provider) {
    synchronized (MUTEX) {
      if (tracerProvider != null) {
        throw new IllegalStateException("TracerProvider has already been initialized.", lastCaller);
      }
      if (Properties.instance().getBoolean("org.nightcode.trace.opentelemetry.disable", false)) {
        provider = TracerProvider.noop();
      }
      tracerProvider = provider;
      lastCaller = new Throwable();
    }
  }

  public static Tracer get(Class<?> clazz) {
    return instance().get(clazz.getName());
  }

  public static TracerProvider instance() {
    TracerProvider provider = tracerProvider;
    if (provider == null) {
      synchronized (MUTEX) {
        provider = tracerProvider;
        if (provider == null) {
          init(TracerProvider.noop());
          provider = tracerProvider;
        }
      }
    }
    return provider;
  }

  public static @Nullable SdkTracerProvider sdkInstance() {
    if (instance() instanceof SdkTracerProvider sdkTracerProvider) {
      return sdkTracerProvider;
    }
    return null;
  }

  public static boolean isNoop() {
    return instance() == TracerProvider.noop();
  }
}
