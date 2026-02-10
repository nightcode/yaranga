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
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.jetbrains.annotations.NotNull;
import org.nightcode.common.base.Jvm;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;
import org.nightcode.common.trace.opentelemetry.exporter.LoggingSpanExporterProvider;
import org.nightcode.common.util.Closeables;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * OpenTelemetry TracerProvider builder.
 */
public final class TracerProviderBuilder {

  private static final AttributeKey<String> SERVICE_NAME    = AttributeKey.stringKey("service.name");
  private static final AttributeKey<String> SERVICE_VERSION = AttributeKey.stringKey("service.version");

  private final ServiceLoader<SpanExporterProvider> serviceLoader = ServiceLoader.load(SpanExporterProvider.class);

  public static TracerProviderBuilder builder() {
    return new TracerProviderBuilder();
  }

  private Resource     resource     = defResource();
  private Sampler      sampler      = defSampler();
  private SpanExporter spanExporter = defSpanExporter();

  private TracerProviderBuilder() {
    // do nothing
  }

  public TracerProviderBuilder resource(@NotNull String serviceName) {
    Objects.requireNonNull(serviceName, "serviceName");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName);
    return resource(Resource.getDefault().merge(Resource.create(attributes)));
  }

  public TracerProviderBuilder resource(@NotNull String serviceName, @NotNull String serviceVersion) {
    Objects.requireNonNull(serviceName, "serviceName");
    Objects.requireNonNull(serviceVersion, "serviceVersion");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName, SERVICE_VERSION, serviceVersion);
    return resource(Resource.getDefault().merge(Resource.create(attributes)));
  }

  public TracerProviderBuilder resource(@NotNull Resource val) {
    Objects.requireNonNull(val, "resource");
    resource = val;
    return this;
  }

  public TracerProviderBuilder sampler(@NotNull Sampler val) {
    Objects.requireNonNull(val, "sampler");
    sampler = val;
    return this;
  }

  public TracerProviderBuilder spanExporter(@NotNull SpanExporter val) {
    Objects.requireNonNull(val, "spanExporter");
    spanExporter = val;
    return this;
  }

  private Resource defResource() {
    String serviceName    = Properties.instance().getString("org.nightcode.trace.service.name", "pid:" + Jvm.pid());
    String serviceVersion = Properties.instance().getString("org.nightcode.trace.service.version", "unknown");

    Attributes attributes = Attributes.of(stringKey("service.name"), serviceName, stringKey("service.version"), serviceVersion);
    return Resource.getDefault().merge(Resource.create(attributes));
  }

  private Sampler defSampler() {
    float probability = Float.parseFloat(Properties.instance().getString("org.nightcode.trace.sampling.ratio", "0.01f"));

    return Sampler.parentBased(Sampler.traceIdRatioBased(probability));
  }

  private SpanExporter defSpanExporter() {
    String spanExporterClassName = Properties.instance().getString("org.nightcode.trace.SpanExporterProvider"
        , LoggingSpanExporterProvider.class.getName());
    return spanExporter(spanExporterClassName);
  }

  public TracerProvider build() {
    if (Properties.instance().getBoolean("org.nightcode.trace.disable", false)) {
      return TracerProvider.noop();
    }

    int  maxQueueSize       = Properties.instance().getInt("org.nightcode.trace.batch.maxQueueSize", 2048);
    int  maxExportBatchSize = Properties.instance().getInt("org.nightcode.trace.batch.maxExportBatchSize", 512);
    long scheduleDelayMs    = Properties.instance().getLong("org.nightcode.trace.batch.scheduleDelayMs", 5_000);
    long exporterTimeoutMs  = Properties.instance().getLong("org.nightcode.trace.batch.exporterTimeoutMs", 30_000);

    SpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
        .setMaxQueueSize(maxQueueSize)
        .setMaxExportBatchSize(maxExportBatchSize)
        .setExporterTimeout(exporterTimeoutMs, TimeUnit.MILLISECONDS)
        .setScheduleDelay(scheduleDelayMs, TimeUnit.MILLISECONDS)
        .build();

    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .setResource(resource)
        .setSampler(sampler)
        .addSpanProcessor(spanProcessor)
        .build();

    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).buildAndRegisterGlobal();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> Closeables.close(sdk)));

    return sdkTracerProvider;
  }

  private SpanExporter spanExporter(String providerClass) {
    Objects.requireNonNull(providerClass, "SpanExporterProvider class name must not be null");
    for (SpanExporterProvider provider : serviceLoader) {
      if (providerClass.equals(provider.getClass().getName())) {
        SpanExporter spanExporter = provider.get();
        Log.info().log(getClass(), "Initialized SpanExporter: {}", spanExporter.getClass().getName());
        return spanExporter;
      }
    }
    Log.warn().log(getClass(), "unable to find SpanExporterProvider {}, fallback to SpanExporter.NOOP", providerClass);
    return SpanExporter.composite();
  }
}
