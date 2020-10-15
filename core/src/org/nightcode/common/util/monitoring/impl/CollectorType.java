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
import org.nightcode.common.util.monitoring.Collector;
import org.nightcode.common.util.monitoring.Counter;
import org.nightcode.common.util.monitoring.Gauge;
import org.nightcode.common.util.monitoring.Histogram;
import org.nightcode.common.util.monitoring.Timer;

/**
 * Collector types.
 */
@Beta
public enum CollectorType {

  COUNTER(Counter.class) {
    @Override CollectorHolder create(CollectorName name, MonitoringManager mm) {
      return new CounterHolder(mm.provider().createCounter(name), mm);
    }

    @Override Collector createChild(Collector collector, String... tagValues) {
      return ((Counter) collector).tags(tagValues);
    }
  },
  GAUGE(Gauge.class) {
    @Override CollectorHolder create(CollectorName name, MonitoringManager mm) {
      return new GaugeHolder(mm.provider().createGauge(name), mm);
    }

    @Override Collector createChild(Collector collector, String... tagValues) {
      throw new UnsupportedOperationException("GAUGE does not support createChild without supplier");
    }
  },
  HISTOGRAM(Histogram.class) {
    @Override CollectorHolder create(CollectorName name, MonitoringManager mm) {
      return new HistogramHolder(mm.provider().createHistogram(name), mm);
    }

    @Override Collector createChild(Collector collector, String... tagValues) {
      return ((Histogram) collector).tags(tagValues);
    }
  },
  TIMER(Timer.class) {
    @Override CollectorHolder create(CollectorName name, MonitoringManager mm) {
      return new TimerHolder(mm.provider().createTimer(name), mm);
    }

    @Override Collector createChild(Collector collector, String... tagValues) {
      return ((Timer) collector).tags(tagValues);
    }
  };

  private final Class<? extends Collector> collectorClass;

  CollectorType(Class<? extends Collector> collectorClass) {
    this.collectorClass = collectorClass;
  }

  Class<? extends Collector> collectorClass() {
    return collectorClass;
  }

  abstract CollectorHolder create(CollectorName name, MonitoringManager mm);

  abstract Collector createChild(Collector collector, String... tagValues);
}
