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
import org.nightcode.common.util.monitoring.Metric;
import org.nightcode.common.util.monitoring.MonitoringContext;

@Beta
class MonitoringContextImpl implements MonitoringContext {

  private final CollectorName metricName;
  private final MonitoringManager monitoringManager;

  MonitoringContextImpl(CollectorName metricName, MonitoringManager monitoringManager) {
    this.metricName = metricName;
    this.monitoringManager = monitoringManager;
  }

  @Override public Metric createMetric(CollectorType type) {
    return new MetricImpl(metricName, null, type, monitoringManager);
  }

  @Override public Metric metric() {
    CollectorHolder collector = monitoringManager.metrics().get(metricName);
    if (collector == null) {
      return null;
    }
    return new MetricImpl(metricName, collector, collector.type(), monitoringManager);
  }

  @Override public void lock() {
    monitoringManager.lock();
  }

  @Override public void unlock() {
    monitoringManager.unlock();
  }
}
