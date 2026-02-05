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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.nightcode.common.pool.SessionPool;

import static java.util.Arrays.asList;

/**
 * Collector for session pools.
 */
public enum SessionPoolsMetrics implements MultiCollector {
  INSTANCE;

  private static final String MN_SESSION_POOL_SIZE    = "session_pool_targets_size";
  private static final String MN_SESSION_POOL_HEALTHY = "session_pool_targets_healthy";

  private static final List<String> METRIC_NAMES = asList(MN_SESSION_POOL_SIZE, MN_SESSION_POOL_HEALTHY);

  public static void addPool(SessionPool<?, ?> pool) {
    INSTANCE.target.add(pool);
  }

  public static void removePool(SessionPool<?, ?> pool) {
    INSTANCE.target.remove(pool);
  }

  public static void register() {
    register(PrometheusRegistry.defaultRegistry);
  }

  public static void register(PrometheusRegistry registry) {
    registry.register(INSTANCE);
  }

  private final CopyOnWriteArrayList<SessionPool<?, ?>> target = new CopyOnWriteArrayList<>();

  @Override public MetricSnapshots collect() {
    // noinspection unchecked
    List<SessionPool<?, ?>> list = (List<SessionPool<?, ?>>) target.clone();
    if (list.isEmpty()) {
      return MetricSnapshots.of();
    }

    List<String> labelNames = List.of("session_pool");

    var total   = CounterSnapshot.builder().name(MN_SESSION_POOL_SIZE);
    var healthy = CounterSnapshot.builder().name(MN_SESSION_POOL_HEALTHY);

    MetricSnapshots.Builder snapshotsBuilder = MetricSnapshots.builder();
    for (SessionPool<?, ?> pool : list) {
      List<String> poolName = Collections.singletonList(pool.poolName());
      Labels       labels   = Labels.of(labelNames, poolName);

      total.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(pool.sessionsTotal()).build());
      healthy.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(pool.sessionsHealthy()).build());
    }

    return snapshotsBuilder
        .metricSnapshot(total.build())
        .metricSnapshot(healthy.build())
        .build();
  }

  @Override public List<String> getPrometheusNames() {
    return METRIC_NAMES;
  }
}
