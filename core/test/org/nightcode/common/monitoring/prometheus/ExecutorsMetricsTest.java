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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.nightcode.common.util.ExecutorUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ExecutorsMetrics}.
 */
public class ExecutorsMetricsTest {

  @Test public void metrics() {
    ExecutorsMetrics.register();
    ExecutorUtils.initialize(ExecutorsMetrics::addExecutor, ExecutorsMetrics::removeExecutor);
    
    try (ThreadPoolExecutor executorService = (ThreadPoolExecutor) ExecutorUtils.singleThreadExecutor("test")) {
      CountDownLatch countDownLatch = new CountDownLatch(1);
      executorService.submit(countDownLatch::countDown);
 
      int attempts = 5;
      while (attempts > 0) {
        ExecutorUtils.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        if (executorService.getCompletedTaskCount() == 1) {
          break;
        }
        attempts--;
      }

      MetricSnapshots snapshots = PrometheusRegistry.defaultRegistry.scrape("executor_completed_task_count"::equals);
      Optional<MetricSnapshot> optional = snapshots.stream().findFirst();
      Assert.assertTrue(optional.isPresent());
      
      MetricSnapshot snapshot = optional.get();
      Assert.assertEquals("executor_completed_task_count", snapshot.getMetadata().getName());

      CounterSnapshot.CounterDataPointSnapshot dataPoint = (CounterSnapshot.CounterDataPointSnapshot) snapshot.getDataPoints().getFirst();
      Assert.assertEquals(1.0, dataPoint.getValue(), 0.0);
      Assert.assertEquals("test-executor", dataPoint.getLabels().get("executor"));
    }
  }
}
