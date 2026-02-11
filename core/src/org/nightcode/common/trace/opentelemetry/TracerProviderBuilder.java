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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

  public static TracerProviderBuilder builder() {
    return new TracerProviderBuilder();
  }

  private static Resource defResource() {
    String serviceName    = Properties.instance().getString("org.nightcode.trace.service.name", "pid:" + Jvm.pid());
    String serviceVersion = Properties.instance().getString("org.nightcode.trace.service.version", "unknown");
    Attributes attributes = Attributes.of(stringKey("service.name"), serviceName, stringKey("service.version"), serviceVersion);
    return Resource.getDefault().merge(Resource.create(attributes));
  }

  private static Sampler defSampler() {
    float probability = Float.parseFloat(Properties.instance().getString("org.nightcode.trace.sampling.ratio", "0.01f"));
    return Sampler.parentBased(Sampler.traceIdRatioBased(probability));
  }

  private static SpanExporter defSpanExporter() {
    String spanExporterClassName = Properties.instance().getString("org.nightcode.trace.SpanExporterProvider"
        , LoggingSpanExporterProvider.class.getName());
    return SpanExporterProvider.spanExporter(spanExporterClassName);
  }

  private Resource     resource;
  private Sampler      sampler;
  private SpanExporter spanExporter;

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

  public TracerProviderBuilder resource(Supplier<Resource> supplier) {
    return resource(supplier.get());
  }

  public TracerProviderBuilder sampler(@NotNull Sampler val) {
    Objects.requireNonNull(val, "sampler");
    sampler = val;
    return this;
  }

  public TracerProviderBuilder sampler(Supplier<Sampler> supplier) {
    return sampler(supplier.get());
  }

  public TracerProviderBuilder spanExporter(@NotNull SpanExporter val) {
    Objects.requireNonNull(val, "spanExporter");
    spanExporter = val;
    return this;
  }

  public TracerProviderBuilder spanExporter(Supplier<SpanExporter> supplier) {
    return spanExporter(supplier.get());
  }

  public TracerProvider build() {
    if (Properties.instance().getBoolean("org.nightcode.trace.disable", false)) {
      return TracerProvider.noop();
    }

    int  maxQueueSize       = Properties.instance().getInt("org.nightcode.trace.batch.maxQueueSize", 2048);
    int  maxExportBatchSize = Properties.instance().getInt("org.nightcode.trace.batch.maxExportBatchSize", 512);
    long scheduleDelayMs    = Properties.instance().getLong("org.nightcode.trace.batch.scheduleDelayMs", 5_000);
    long exporterTimeoutMs  = Properties.instance().getLong("org.nightcode.trace.batch.exporterTimeoutMs", 30_000);

    Resource resource = this.resource;
    if (resource == null) {
      resource = defResource();
    }

    Sampler sampler = this.sampler;
    if (sampler == null) {
      sampler = defSampler();
    }

    SpanExporter spanExporter = this.spanExporter;
    if (spanExporter == null) {
      spanExporter = defSpanExporter();
    }

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

  public TracerProvider register() {
    TracerProvider tracerProvider = build();
    org.nightcode.common.trace.opentelemetry.TracerProvider.init(tracerProvider);
    return tracerProvider;
  }
}
