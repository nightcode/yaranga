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

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.nightcode.common.logging.Log;

/**
 * SpanExporter provider.
 */
public interface SpanExporterProvider extends Supplier<SpanExporter> {

  ServiceLoader<SpanExporterProvider> SERVICE_LOADER = ServiceLoader.load(SpanExporterProvider.class);

  static SpanExporter spanExporter(String providerClass) {
    Objects.requireNonNull(providerClass, "SpanExporterProvider class name must not be null");
    for (SpanExporterProvider provider : SERVICE_LOADER) {
      if (providerClass.equals(provider.getClass().getName())) {
        SpanExporter spanExporter = provider.get();
        Log.info().log(SpanExporterProvider.class, "Initialized SpanExporter: {}", spanExporter.getClass().getName());
        return spanExporter;
      }
    }
    Log.warn().log(SpanExporterProvider.class, "unable to find SpanExporterProvider {}, fallback to SpanExporter.NOOP", providerClass);
    return SpanExporter.composite();
  }
}
