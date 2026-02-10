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

package org.nightcode.common.metrics.prometheus;

import java.util.Collections;
import java.util.Date;

import io.prometheus.metrics.config.PrometheusProperties;
import io.prometheus.metrics.core.metrics.MetricWithFixedMetadata;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple Application ino collector.
 */
public final class AppInfoMetrics extends MetricWithFixedMetadata {

  public static final String FULL_NAME = "appinfo";

  public static final class Builder extends MetricWithFixedMetadata.Builder<Builder, AppInfoMetrics> {
    private String appName;
    private String appVersion;

    private Builder(PrometheusProperties config) {
      super(Collections.emptyList(), config);
      name(FULL_NAME);
    }

    @Override public Builder unit(@Nullable Unit unit) {
      if (unit != null) {
        throw new UnsupportedOperationException("AppInfoMetrics metrics cannot have a unit.");
      }
      return this;
    }

    public Builder appName(@NotNull String val) {
      appName = val;
      return this;
    }

    public Builder appVersion(@NotNull String val) {
      appVersion = val;
      return this;
    }

    @Override public AppInfoMetrics build() {
      if (appName == null || appVersion == null) {
        throw new IllegalArgumentException("appName and appVersion must not be null");
      }
      return new AppInfoMetrics(this);
    }

    @Override protected Builder self() {
      return this;
    }
  }

  public static Builder builder() {
    return builder(PrometheusProperties.get());
  }

  public static Builder builder(PrometheusProperties config) {
    return new Builder(config);
  }

  private final MetricSnapshot snapshot;

  private AppInfoMetrics(Builder builder) {
    super(builder);

    Labels labels = Labels.of("name", builder.appName, "version", builder.appVersion);

    var snapshotBuilder = GaugeSnapshot.GaugeDataPointSnapshot.builder();
    snapshotBuilder.labels(labels).value(new Date().getTime());

    snapshot = GaugeSnapshot.builder()
        .name(FULL_NAME)
        .help("Application information (boot timestamp, ms.)")
        .dataPoint(snapshotBuilder.build())
        .build();
  }

  @Override public MetricSnapshot collect() {
    return snapshot;
  }
}
