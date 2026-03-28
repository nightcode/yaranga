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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.nightcode.common.base.Jvm;
import org.nightcode.common.props.Properties;

/**
 * OpenTelemetry LoggerProvider builder.
 */
public final class OtelLoggerProviderBuilder {

  private static final String PROP_PREFIX = "org.nightcode.logging.opentelemetry.";

  private static final AttributeKey<String> SERVICE_NAME    = AttributeKey.stringKey("service.name");
  private static final AttributeKey<String> SERVICE_VERSION = AttributeKey.stringKey("service.version");

  public static OtelLoggerProviderBuilder builder() {
    return new OtelLoggerProviderBuilder();
  }

  private static Resource defResource() {
    String serviceName    = Properties.instance().getString("org.nightcode.opentelemetry.service.name", "pid:" + Jvm.pid());
    String serviceVersion = Properties.instance().getString("org.nightcode.opentelemetry.service.version", "unknown");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName, SERVICE_VERSION, serviceVersion);
    return Resource.getDefault().merge(Resource.create(attributes));
  }

  private static LogRecordExporter defLogRecordExporter() {
    return  LogRecordExporter.composite();
  }

  private Resource          resource;
  private LogRecordExporter logRecordExporter;

  private OtelLoggerProviderBuilder() {
    // do nothing
  }

  public OtelLoggerProviderBuilder resource(@NotNull String serviceName) {
    Objects.requireNonNull(serviceName, "serviceName");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName);
    return resource(Resource.getDefault().merge(Resource.create(attributes)));
  }

  public OtelLoggerProviderBuilder resource(@NotNull String serviceName, @NotNull String serviceVersion) {
    Objects.requireNonNull(serviceName, "serviceName");
    Objects.requireNonNull(serviceVersion, "serviceVersion");
    Attributes attributes = Attributes.of(SERVICE_NAME, serviceName, SERVICE_VERSION, serviceVersion);
    return resource(Resource.getDefault().merge(Resource.create(attributes)));
  }

  public OtelLoggerProviderBuilder resource(@NotNull Resource val) {
    Objects.requireNonNull(val, "resource");
    resource = val;
    return this;
  }

  public OtelLoggerProviderBuilder resource(Supplier<Resource> supplier) {
    return resource(supplier.get());
  }

  public OtelLoggerProviderBuilder logRecordExporter(@NotNull LogRecordExporter val) {
    Objects.requireNonNull(val, "logRecordExporter");
    logRecordExporter = val;
    return this;
  }

  public OtelLoggerProviderBuilder logRecordExporter(Supplier<LogRecordExporter> supplier) {
    return logRecordExporter(supplier.get());
  }

  public LoggerProvider build() {
    if (Properties.instance().getBoolean(PROP_PREFIX + "disable", false)) {
      return LoggerProvider.noop();
    }

    int  maxQueueSize       = Properties.instance().getInt(PROP_PREFIX + "batch.maxQueueSize", 2048);
    int  maxExportBatchSize = Properties.instance().getInt(PROP_PREFIX + "batch.maxExportBatchSize", 512);
    long scheduleDelayMs    = Properties.instance().getLong(PROP_PREFIX + "batch.scheduleDelayMs", 5_000);
    long exporterTimeoutMs  = Properties.instance().getLong(PROP_PREFIX + "batch.exporterTimeoutMs", 30_000);

    Resource resource = this.resource;
    if (resource == null) {
      resource = defResource();
    }

    LogRecordExporter logRecordExporter = this.logRecordExporter;
    if (logRecordExporter == null) {
      logRecordExporter = defLogRecordExporter();
    }

    LogRecordProcessor logRecordProcessor = BatchLogRecordProcessor.builder(logRecordExporter)
        .setMaxQueueSize(maxQueueSize)
        .setMaxExportBatchSize(maxExportBatchSize)
        .setExporterTimeout(exporterTimeoutMs, TimeUnit.MILLISECONDS)
        .setScheduleDelay(scheduleDelayMs, TimeUnit.MILLISECONDS)
        .build();

    return SdkLoggerProvider.builder()
        .setResource(resource)
        .addLogRecordProcessor(logRecordProcessor)
        .build();
  }

  public LoggerProvider register() {
    LoggerProvider provider = build();
    OtelLoggerProvider.init(provider);
    return provider;
  }
}
