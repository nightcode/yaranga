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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;

import static java.util.Arrays.asList;

/**
 * API gateway collector.
 */
public enum ApiGwMetrics implements MultiCollector {
  INSTANCE;

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
    INSTANCE.target.add(apiGateway);
    return apiGateway;
  }

  public static void removeApiGw(ApiGw apiGateway) {
    INSTANCE.target.remove(apiGateway);
  }

  public static void register() {
    register(PrometheusRegistry.defaultRegistry);
  }

  public static void register(PrometheusRegistry registry) {
    registry.register(INSTANCE);
  }

  private final CopyOnWriteArrayList<ApiGw> target = new CopyOnWriteArrayList<>();

  @Override public MetricSnapshots collect() {
    // noinspection unchecked
    List<ApiGw> list = (List<ApiGw>) target.clone();
    if (list.isEmpty()) {
      return MetricSnapshots.of();
    }

    List<String> labelNames = List.of("api_gw");

    var requests              = CounterSnapshot.builder().name(MN_API_GW_REQUESTS);
    var acceptorExecutorCount = CounterSnapshot.builder().name(MN_API_GW_ACCEPTOR_EXECUTOR_COUNT);
    var acceptorPendingTasks  = CounterSnapshot.builder().name(MN_API_GW_ACCEPTOR_PENDING_TASKS);
    var workerExecutorCount   = CounterSnapshot.builder().name(MN_API_GW_WORKER_EXECUTOR_COUNT);
    var workerPendingTasks    = CounterSnapshot.builder().name(MN_API_GW_WORKER_PENDING_TASKS);

    MetricSnapshots.Builder snapshotsBuilder = MetricSnapshots.builder();
    for (ApiGw apiGw : list) {
      List<String> apiGwName = Collections.singletonList(apiGw.name());
      Labels       labels    = Labels.of(labelNames, apiGwName);

      requests.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(apiGw.requestsCount()).build());
      acceptorExecutorCount.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(apiGw.acceptorGroup().executorCount())
          .build());
      workerExecutorCount.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(apiGw.workerGroup().executorCount()).build());
      addPendingTasksMetric(labels, apiGw.acceptorGroup(), acceptorPendingTasks);
      addPendingTasksMetric(labels, apiGw.workerGroup(), workerPendingTasks);
    }

    return snapshotsBuilder
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

  private void addPendingTasksMetric(Labels labels, EventExecutorGroup target, CounterSnapshot.Builder builder) {
    EventExecutor           eventExecutor;
    Iterator<EventExecutor> i = target.iterator();
    if (i.hasNext() && (eventExecutor = i.next()) instanceof SingleThreadEventLoop) {
      SingleThreadEventLoop eventLoop    = (SingleThreadEventLoop) eventExecutor;
      int                   pendingTasks = eventLoop.pendingTasks();
      while (i.hasNext()) {
        eventLoop = (SingleThreadEventLoop) i.next();
        pendingTasks += eventLoop.pendingTasks();
      }
      builder.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(pendingTasks).build());
    }
  }
}
