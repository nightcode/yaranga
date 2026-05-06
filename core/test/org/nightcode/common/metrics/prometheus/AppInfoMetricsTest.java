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

import java.util.Optional;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.Unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link AppInfoMetrics}.
 */
public class AppInfoMetricsTest {

  @Test public void metrics() {
    AppInfoMetrics.builder().appName("testName").appVersion("testVersion").register();

    MetricSnapshots snapshots = PrometheusRegistry.defaultRegistry.scrape(AppInfoMetrics.FULL_NAME::equals);
    Optional<MetricSnapshot> optional = snapshots.stream().findFirst();
    assertTrue(optional.isPresent());

    MetricSnapshot    snapshot  = optional.get();
    DataPointSnapshot dataPoint = snapshot.getDataPoints().getFirst();
    assertEquals("testName", dataPoint.getLabels().get("name"));
    assertEquals("testVersion", dataPoint.getLabels().get("version"));
  }

  @Test public void unit() {
    try {
      AppInfoMetrics.builder().unit(Unit.SECONDS);
      fail("should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException ex) {
      assertEquals("AppInfoMetrics metrics cannot have a unit.", ex.getMessage());
    }
  }

  @Test public void buildWithoutName() {
    try {
      AppInfoMetrics.builder().appVersion("testVersion").register();
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("appName and appVersion must not be null", ex.getMessage());
    }
  }

  @Test public void buildWithoutVersion() {
    try {
      AppInfoMetrics.builder().appName("testName").register();
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("appName and appVersion must not be null", ex.getMessage());
    }
  }
}
