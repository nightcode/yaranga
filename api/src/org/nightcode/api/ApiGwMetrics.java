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

package org.nightcode.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;

import static java.util.Arrays.asList;

/**
 * API gateway collector.
 */
public enum ApiGwMetrics implements MultiCollector {
  INSTANCE;

  private static final String LABEL_API_GW = "api_gw";

  private static final String MN_API_GW_REQUESTS                = "nc_api_gw_requests";
  private static final String MN_API_GW_ACCEPTOR_EXECUTOR_COUNT = "nc_api_gw_acceptor_executor_count";
  private static final String MN_API_GW_ACCEPTOR_PENDING_TASKS  = "nc_api_gw_acceptor_pending_tasks";
  private static final String MN_API_GW_WORKER_EXECUTOR_COUNT   = "nc_api_gw_worker_executor_count";
  private static final String MN_API_GW_WORKER_PENDING_TASKS    = "nc_api_gw_worker_pending_tasks";

  private static final List<String> METRIC_NAMES = asList(
      MN_API_GW_REQUESTS
      , MN_API_GW_ACCEPTOR_EXECUTOR_COUNT
      , MN_API_GW_ACCEPTOR_PENDING_TASKS
      , MN_API_GW_WORKER_EXECUTOR_COUNT
      , MN_API_GW_WORKER_PENDING_TASKS);

  public static ApiGw addApiGw(ApiGw apiGateway) {
    INSTANCE.targets.add(apiGateway);
    return apiGateway;
  }

  public static void removeApiGw(ApiGw apiGateway) {
    INSTANCE.targets.remove(apiGateway);
  }

  public static void register() {
    register(PrometheusRegistry.defaultRegistry);
  }

  public static void register(PrometheusRegistry registry) {
    registry.register(INSTANCE);
  }

  private static CounterDataPointSnapshot counterDataPoint(Labels labels, double value) {
    return CounterDataPointSnapshot.builder().labels(labels).value(value).build();
  }

  private static GaugeSnapshot.GaugeDataPointSnapshot gaugeDataPoint(Labels labels, double value) {
    return GaugeSnapshot.GaugeDataPointSnapshot.builder().labels(labels).value(value).build();
  }

  private final List<ApiGw> targets = new CopyOnWriteArrayList<>();

  @Override public MetricSnapshots collect() {
    if (targets.isEmpty()) {
      return MetricSnapshots.of();
    }

    var requests              = CounterSnapshot.builder().name(MN_API_GW_REQUESTS);
    var acceptorExecutorCount = GaugeSnapshot.builder().name(MN_API_GW_ACCEPTOR_EXECUTOR_COUNT);
    var acceptorPendingTasks  = GaugeSnapshot.builder().name(MN_API_GW_ACCEPTOR_PENDING_TASKS);
    var workerExecutorCount   = GaugeSnapshot.builder().name(MN_API_GW_WORKER_EXECUTOR_COUNT);
    var workerPendingTasks    = GaugeSnapshot.builder().name(MN_API_GW_WORKER_PENDING_TASKS);

    for (ApiGw apiGw : targets) {
      Labels labels = Labels.of(LABEL_API_GW, apiGw.name());

      requests.dataPoint(counterDataPoint(labels, apiGw.requestsCount()));
      collectGroupMetrics(labels, apiGw.acceptorGroup(), acceptorExecutorCount, acceptorPendingTasks);
      collectGroupMetrics(labels, apiGw.workerGroup(), workerExecutorCount, workerPendingTasks);
    }

    return MetricSnapshots.builder()
        .metricSnapshot(requests.build())
        .metricSnapshot(acceptorExecutorCount.build())
        .metricSnapshot(acceptorPendingTasks.build())
        .metricSnapshot(workerExecutorCount.build())
        .metricSnapshot(workerPendingTasks.build())
        .build();
  }

  @Override public List<String> getPrometheusNames() {
    return METRIC_NAMES;
  }

  private void collectGroupMetrics(Labels labels, EventExecutorGroup group, GaugeSnapshot.Builder executorCount,
                                   GaugeSnapshot.Builder pendingTasks) {
    int     executors        = 0;
    long    pending          = 0;
    boolean pendingSupported = false;

    for (EventExecutor executor : group) {
      executors++;
      if (executor instanceof SingleThreadEventLoop eventLoop) {
        pendingSupported = true;
        pending += eventLoop.pendingTasks();
      }
    }

    executorCount.dataPoint(gaugeDataPoint(labels, executors));
    if (pendingSupported) {
      pendingTasks.dataPoint(gaugeDataPoint(labels, pending));
    }
  }
}
