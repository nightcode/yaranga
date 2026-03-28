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
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.jetbrains.annotations.NotNull;
import org.nightcode.common.base.Jvm;
import org.nightcode.common.props.Properties;

/**
 * OpenTelemetry TracerProvider builder.
 */
public final class OtelTracerProviderBuilder {

  private static final String PROP_PREFIX = "org.nightcode.trace.opentelemetry.";

  private static final AttributeKey<String> SERVICE_NAME    = AttributeKey.stringKey("service.name");
  private static final AttributeKey<String> SERVICE_VERSION = AttributeKey.stringKey("service.version");

  public static OtelTracerProviderBuilder builder() {
    return new OtelTracerProviderBuilder();
  }

  private static Resource defResource() {
    String serviceName    = Properties.instance().getString("org.nightcode.opentelemetry.service.name", "pid:" + Jvm.pid());
    String serviceVersion = Properties.instance().getString("org.nightcode.opentelemetry.service.version", "unknown");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName, SERVICE_VERSION, serviceVersion);
    return Resource.getDefault().merge(Resource.create(attributes));
  }

  private static Sampler defSampler() {
    float probability = Float.parseFloat(Properties.instance().getString(PROP_PREFIX + "sampling.ratio", "0.01f"));
    return Sampler.parentBased(Sampler.traceIdRatioBased(probability));
  }

  private static SpanExporter defSpanExporter() {
    return new LoggingSpanExporter();
  }

  private Resource     resource;
  private Sampler      sampler;
  private SpanExporter spanExporter;

  private OtelTracerProviderBuilder() {
    // do nothing
  }

  public OtelTracerProviderBuilder resource(@NotNull String serviceName) {
    Objects.requireNonNull(serviceName, "serviceName");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName);
    return resource(Resource.getDefault().merge(Resource.create(attributes)));
  }

  public OtelTracerProviderBuilder resource(@NotNull String serviceName, @NotNull String serviceVersion) {
    Objects.requireNonNull(serviceName, "serviceName");
    Objects.requireNonNull(serviceVersion, "serviceVersion");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName, SERVICE_VERSION, serviceVersion);
    return resource(Resource.getDefault().merge(Resource.create(attributes)));
  }

  public OtelTracerProviderBuilder resource(@NotNull Resource val) {
    Objects.requireNonNull(val, "resource");
    resource = val;
    return this;
  }

  public OtelTracerProviderBuilder resource(Supplier<Resource> supplier) {
    return resource(supplier.get());
  }

  public OtelTracerProviderBuilder sampler(@NotNull Sampler val) {
    Objects.requireNonNull(val, "sampler");
    sampler = val;
    return this;
  }

  public OtelTracerProviderBuilder sampler(Supplier<Sampler> supplier) {
    return sampler(supplier.get());
  }

  public OtelTracerProviderBuilder spanExporter(@NotNull SpanExporter val) {
    Objects.requireNonNull(val, "spanExporter");
    spanExporter = val;
    return this;
  }

  public OtelTracerProviderBuilder spanExporter(Supplier<SpanExporter> supplier) {
    return spanExporter(supplier.get());
  }

  public TracerProvider build() {
    if (Properties.instance().getBoolean(PROP_PREFIX + "disable", false)) {
      return TracerProvider.noop();
    }

    int  maxQueueSize       = Properties.instance().getInt(PROP_PREFIX + "batch.maxQueueSize", 2048);
    int  maxExportBatchSize = Properties.instance().getInt(PROP_PREFIX + "batch.maxExportBatchSize", 512);
    long scheduleDelayMs    = Properties.instance().getLong(PROP_PREFIX + "batch.scheduleDelayMs", 5_000);
    long exporterTimeoutMs  = Properties.instance().getLong(PROP_PREFIX + "batch.exporterTimeoutMs", 30_000);

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

    return SdkTracerProvider.builder()
        .setResource(resource)
        .setSampler(sampler)
        .addSpanProcessor(spanProcessor)
        .build();
  }

  public TracerProvider register() {
    TracerProvider tracerProvider = build();
    OtelTracerProvider.init(tracerProvider);
    return tracerProvider;
  }
}
