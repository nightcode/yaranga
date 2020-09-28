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

package org.nightcode.common.util.monitoring;

import org.nightcode.common.annotations.Beta;

import java.util.concurrent.TimeUnit;

/**
 *  Null implementation of {@link MonitoringProvider} interface.
 */
@Beta
class NullMonitoringProvider implements MonitoringProvider {

  private static final Counter COUNTER = new Counter() {
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
      return -1;
    }
  };

  private static final Histogram HISTOGRAM = new Histogram() {
    @Override public void update(int value) {
      // do nothing
    }

    @Override public void update(long value) {
      // do nothing
    }
  };

  private static final Timer TIMER = new Timer() {
    @Override public Context startTimer() {
      return () -> -1;
    }

    @Override public void update(long duration, TimeUnit unit) {
      // do nothing
    }
  };

  @Override public boolean deregister(String name) {
    return true;
  }

  @Override public Counter registerCounter(String name) {
    return COUNTER;
  }

  @Override public <T> void registerGauge(String name, Gauge<T> gauge) {
    // do nothing
  }

  @Override public Histogram registerHistogram(String name) {
    return HISTOGRAM;
  }

  @Override public Timer registerTimer(String name) {
    return TIMER;
  }

  @Override public String name() {
    return NullMonitoringProvider.class.getSimpleName();
  }
}
