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
import org.nightcode.common.util.monitoring.MonitoringEngine;
import org.nightcode.common.util.monitoring.Timer;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 *  Null implementation of {@link MonitoringEngine} interface.
 */
@Beta
class NullMonitoringEngine implements MonitoringEngine {

  @Override public boolean deregister(Collector metric) {
    return true;
  }

  @Override public Counter createCounter(CollectorName name) {
    return new Counter() {
      @Override public void inc() {
        // do nothing
      }

      @Override public void inc(long value) {
        // do nothing
      }

      @Override public void dec() {
        // do nothing
      }

      @Override public void dec(long value) {
        // do nothing
      }

      @Override public long getCount() {
        return 0;
      }

      @Override public Child tags(String... tagValues) {
        return new Child() {
          @Override public void inc() {
            // do nothing
          }

          @Override public void inc(long value) {
            // do nothing
          }

          @Override public void dec() {
            // do nothing
          }

          @Override public void dec(long value) {
            // do nothing
          }

          @Override public long getCount() {
            return 0;
          }

          @Override public CollectorName name() {
            return CollectorName.of(name, tagValues);
          }
        };
      }

      @Override public CollectorName name() {
        return name;
      }
    };
  }

  @Override public Gauge createGauge(CollectorName name) {
    return new Gauge() {
      @Override public CollectorName name() {
        return name;
      }

      @Override public Child tags(Supplier<?> gauge, String... tagValues) {
        return () -> CollectorName.of(name, tagValues);
      }
    };
  }

  @Override public Gauge createGauge(CollectorName name, Supplier<?> gauge) {
    return new Gauge() {
      @Override public CollectorName name() {
        return name;
      }

      @Override public Child tags(Supplier<?> gauge, String... tagValues) {
        return () -> CollectorName.of(name, tagValues);
      }
    };
  }

  @Override public Histogram createHistogram(CollectorName name) {
    return new Histogram() {
      @Override public Child tags(String... tagValues) {
        return new Child() {
          @Override public CollectorName name() {
            return CollectorName.of(name, tagValues);
          }

          @Override public void update(int value) {
            // do nothing
          }

          @Override public void update(long value) {
            // do nothing
          }
        };
      }

      @Override public CollectorName name() {
        return name;
      }

      @Override public void update(int value) {
        // do nothing
      }

      @Override public void update(long value) {
        // do nothing
      }
    };
  }

  @Override public Timer createTimer(CollectorName name) {
    return new Timer() {
      @Override public Child tags(String... tagValues) {
        return new Child() {
          @Override public CollectorName name() {
            return CollectorName.of(name, tagValues);
          }

          @Override public Context startTimer() {
            return () -> 0;
          }

          @Override public void update(long duration, TimeUnit unit) {
            // do nothing
          }
        };
      }

      @Override public CollectorName name() {
        return name;
      }

      @Override public Context startTimer() {
        return () -> 0;
      }

      @Override public void update(long duration, TimeUnit unit) {
        // do nothing
      }
    };
  }

  @Override public char nameSeparator() {
    return '.';
  }
}
