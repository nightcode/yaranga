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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;

import static java.util.Arrays.asList;

/**
 * Collector for executors.
 */
public enum ExecutorsMetrics implements MultiCollector {
  INSTANCE;

  private static class MetricsExecutor {
    private final ExecutorService delegate;

    MetricsExecutor(ExecutorService delegate) {
      this.delegate = delegate;
    }

    ThreadFactory threadFactory() {
      return null;
    }

    int activeCount() {
      return 0;
    }

    long completedTaskCount() {
      return 0L;
    }

    int corePoolSize() {
      return 0;
    }

    int largestPoolSize() {
      return 0;
    }

    int maximumPoolSize() {
      return 0;
    }

    int poolSize() {
      return 0;
    }

    long taskCount() {
      return 0L;
    }

    int queueSize() {
      return 0;
    }

    int queueRemainingCapacity() {
      return 0;
    }

    @Override public final int hashCode() {
      return delegate.hashCode();
    }

    @Override public final boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof MetricsExecutor other)) {
        return false;
      }
      return delegate.equals(other.delegate);
    }
  }

  private static MetricsExecutor of(ExecutorService delegate) {
    return new MetricsExecutor(delegate);
  }

  private static MetricsExecutor of(ThreadPoolExecutor delegate) {
    return new MetricsExecutor(delegate) {
      @Override public int activeCount() {
        return delegate.getActiveCount();
      }

      @Override public long completedTaskCount() {
        return delegate.getCompletedTaskCount();
      }

      @Override public int corePoolSize() {
        return delegate.getCorePoolSize();
      }

      @Override public int largestPoolSize() {
        return delegate.getLargestPoolSize();
      }

      @Override public int maximumPoolSize() {
        return delegate.getMaximumPoolSize();
      }

      @Override public int poolSize() {
        return delegate.getPoolSize();
      }

      @Override public long taskCount() {
        return delegate.getTaskCount();
      }

      @Override public ThreadFactory threadFactory() {
        return delegate.getThreadFactory();
      }

      @Override public int queueSize() {
        return delegate.getQueue().size();
      }

      @Override public int queueRemainingCapacity() {
        return delegate.getQueue().remainingCapacity();
      }
    };
  }

  private static final String MN_EXECUTOR_ACTIVE_COUNT             = "nc_executor_active_count";
  private static final String MN_EXECUTOR_COMPLETED_TASK_COUNT     = "nc_executor_completed_task_count";
  private static final String MN_EXECUTOR_CORE_POOL_SIZE           = "nc_executor_core_pool_size";
  private static final String MN_EXECUTOR_LARGEST_POOL_SIZE        = "nc_executor_largest_pool_size";
  private static final String MN_EXECUTOR_MAXIMUM_POOL_SIZE        = "nc_executor_maximum_pool_size";
  private static final String MN_EXECUTOR_POOL_SIZE                = "nc_executor_pool_size";
  private static final String MN_EXECUTOR_TASK_COUNT               = "nc_executor_task_count";
  private static final String MN_EXECUTOR_QUEUE_SIZE               = "nc_executor_queue_size";
  private static final String MN_EXECUTOR_QUEUE_REMAINING_CAPACITY = "nc_executor_queue_remaining_capacity";

  private static final List<String> METRIC_NAMES = asList(
      MN_EXECUTOR_ACTIVE_COUNT
      , MN_EXECUTOR_COMPLETED_TASK_COUNT
      , MN_EXECUTOR_CORE_POOL_SIZE
      , MN_EXECUTOR_LARGEST_POOL_SIZE
      , MN_EXECUTOR_MAXIMUM_POOL_SIZE
      , MN_EXECUTOR_POOL_SIZE
      , MN_EXECUTOR_TASK_COUNT
      , MN_EXECUTOR_QUEUE_SIZE
      , MN_EXECUTOR_QUEUE_REMAINING_CAPACITY);

  public static <T extends ThreadPoolExecutor> T addExecutor(T executorService) {
    INSTANCE.target.add(of(executorService));
    return executorService;
  }

  public static void removeExecutor(ExecutorService executorService) {
    INSTANCE.target.remove(of(executorService));
  }

  public static void register() {
    register(PrometheusRegistry.defaultRegistry);
  }

  public static void register(PrometheusRegistry registry) {
    registry.register(INSTANCE);
  }

  private final CopyOnWriteArrayList<MetricsExecutor> target = new CopyOnWriteArrayList<>();

  @Override public MetricSnapshots collect() {
    // noinspection unchecked
    List<MetricsExecutor> list = (List<MetricsExecutor>) target.clone();
    if (list.isEmpty()) {
      return MetricSnapshots.of();
    }

    List<String> labelNames = List.of("executor");

    var activeCount            = CounterSnapshot.builder().name(MN_EXECUTOR_ACTIVE_COUNT);
    var completedTaskCount     = CounterSnapshot.builder().name(MN_EXECUTOR_COMPLETED_TASK_COUNT);
    var corePoolSize           = CounterSnapshot.builder().name(MN_EXECUTOR_CORE_POOL_SIZE);
    var largestPoolSize        = CounterSnapshot.builder().name(MN_EXECUTOR_LARGEST_POOL_SIZE);
    var maximumPoolSize        = CounterSnapshot.builder().name(MN_EXECUTOR_MAXIMUM_POOL_SIZE);
    var poolSize               = CounterSnapshot.builder().name(MN_EXECUTOR_POOL_SIZE);
    var taskCount              = CounterSnapshot.builder().name(MN_EXECUTOR_TASK_COUNT);
    var queueSize              = CounterSnapshot.builder().name(MN_EXECUTOR_QUEUE_SIZE);
    var queueRemainingCapacity = CounterSnapshot.builder().name(MN_EXECUTOR_QUEUE_REMAINING_CAPACITY);

    MetricSnapshots.Builder snapshotsBuilder = MetricSnapshots.builder();
    for (MetricsExecutor executor : list) {
      List<String> executorName = Collections.singletonList(String.valueOf(executor.threadFactory()));
      Labels       labels       = Labels.of(labelNames, executorName);

      activeCount.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.activeCount()).build());
      completedTaskCount.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.completedTaskCount()).build());
      corePoolSize.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.corePoolSize()).build());
      largestPoolSize.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.largestPoolSize()).build());
      maximumPoolSize.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.maximumPoolSize()).build());
      poolSize.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.poolSize()).build());
      taskCount.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.taskCount()).build());
      queueSize.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.queueSize()).build());
      queueRemainingCapacity.dataPoint(CounterDataPointSnapshot.builder().labels(labels).value(executor.queueRemainingCapacity()).build());
    }

    return snapshotsBuilder
        .metricSnapshot(activeCount.build())
        .metricSnapshot(completedTaskCount.build())
        .metricSnapshot(corePoolSize.build())
        .metricSnapshot(largestPoolSize.build())
        .metricSnapshot(maximumPoolSize.build())
        .metricSnapshot(poolSize.build())
        .metricSnapshot(taskCount.build())
        .metricSnapshot(queueSize.build())
        .metricSnapshot(queueRemainingCapacity.build())
        .build();
  }

  @Override public List<String> getPrometheusNames() {
    return METRIC_NAMES;
  }
}
