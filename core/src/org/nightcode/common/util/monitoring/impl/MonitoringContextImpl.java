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

package org.nightcode.common.util.monitoring.impl;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.util.monitoring.CollectorHolder;
import org.nightcode.common.util.monitoring.Metric;
import org.nightcode.common.util.monitoring.MonitoringContext;
import org.nightcode.common.util.monitoring.MonitoringEngine;
import org.nightcode.common.util.monitoring.MonitoringProvider;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Beta
class MonitoringContextImpl implements MonitoringContext {

  private final CollectorName metricName;
  private final ReentrantLock lock;
  private final Map<CollectorName, CollectorHolder> metrics;
  private final MonitoringProvider provider;

  MonitoringContextImpl(CollectorName metricName, ReentrantLock lock, Map<CollectorName, CollectorHolder> metrics,
      MonitoringProvider provider) {
    this.metricName = metricName;
    this.lock = lock;
    this.metrics = metrics;
    this.provider = provider;
  }

  @Override public Metric createMetric(CollectorType type) {
    return new MetricImpl(metricName, null, type, this);
  }

  @Override public MonitoringEngine engine() {
    return provider.engine();
  }

  @Override public Metric metric() {
    CollectorHolder collector = metrics.get(metricName);
    if (collector == null) {
      return null;
    }
    return new MetricImpl(metricName, collector, collector.type(), this);
  }

  @Override public Map<CollectorName, CollectorHolder> metrics() {
    return metrics;
  }

  @Override public MonitoringProvider provider() {
    return provider;
  }

  @Override public void lock() {
    lock.lock();
  }

  @Override public void unlock() {
    lock.unlock();
  }
}
