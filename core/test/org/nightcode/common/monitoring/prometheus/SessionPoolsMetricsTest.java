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

package org.nightcode.common.monitoring.prometheus;

import java.util.Optional;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.nightcode.common.pool.AbstractSession;
import org.nightcode.common.pool.Session;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.SessionPoolBuilder;
import org.nightcode.common.pool.metadata.NamedEndpoint;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SessionPoolsMetrics}.
 */
public class SessionPoolsMetricsTest {

  @Test public void metrics() {
    SessionPoolsMetrics.register();

    try (SessionPool<String, Session<String>> pool = SessionPoolBuilder.<String, Session<String>>instance("testPool")
        .addEndpoint(new NamedEndpoint("testEndpoint"))
        .sessionFactory(context -> new AbstractSession<>(context.endpoint()) {
          @Override protected void createImpl() {
            // do nothing
          }

          @Override protected void destroyImpl() {
            // do nothing
          }
        })
        .build()) {
      pool.init();

      SessionPoolsMetrics.addPool(pool);

      MetricSnapshots          snapshots = PrometheusRegistry.defaultRegistry.scrape("session_pool_targets_size"::equals);
      Optional<MetricSnapshot> optional  = snapshots.stream().findFirst();
      Assert.assertTrue(optional.isPresent());

      MetricSnapshot snapshot = optional.get();
      Assert.assertEquals("session_pool_targets_size", snapshot.getMetadata().getName());

      CounterSnapshot.CounterDataPointSnapshot dataPoint = (CounterSnapshot.CounterDataPointSnapshot) snapshot.getDataPoints().getFirst();
      Assert.assertEquals(1.0, dataPoint.getValue(), 0.0);
      Assert.assertEquals("testPool", dataPoint.getLabels().get("session_pool"));

      SessionPoolsMetrics.removePool(pool);
    }
  }
}
