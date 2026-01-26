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

package org.nightcode.common.tracing;

import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;

/**
 * Base TracerProvider interface.
 */
public interface TracerProvider extends Supplier<CloseableTracer> {

  ServiceLoader<TracerProvider> SERVICE_LOADER = ServiceLoader.load(TracerProvider.class);

  static CloseableTracer instance() {
    String providerClass = Properties.instance().getString("org.nightcode.tracing.TracerProvider", NoopTracerProvider.class.getName());
    for (TracerProvider provider : SERVICE_LOADER) {
      if (providerClass.equals(provider.getClass().getName())) {
        CloseableTracer tracer = provider.get();
        Log.info().log(TracerProvider.class, "Initialized Tracer: {}", tracer.getClass().getName());
        return tracer;
      }
    }
    Log.warn().log(TracerProvider.class, "unable to find TracerProvider {}, fallback to Tracer.NOOP", providerClass);
    return NoopTracerProvider.NOOP;
  }
}
