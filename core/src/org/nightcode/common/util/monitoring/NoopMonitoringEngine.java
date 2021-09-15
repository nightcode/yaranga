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
import java.util.function.Supplier;

/**
 *  Null implementation of {@link MonitoringEngine} interface.
 */
@Beta
public final class NoopMonitoringEngine implements MonitoringEngine {

  @Override public boolean deregister(Collector collector) {
    return true;
  }

  @Override public <C extends Collector> C register(Supplier<C> supplier) {
    return supplier.get();
  }

  @Override public <C extends Collector> void registerSilent(Supplier<C> supplier) {
    // do nothing
  }

  @Override public Counter registerCounter(String name, String help, String... tagNames) {
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
        };
      }
    };
  }

  @Override public Histogram registerHistogram(String name, String help, String... tagNames) {
    return new Histogram() {
      @Override public Child tags(String... tagValues) {
        return new Child() {
          @Override public long count() {
            return 0;
          }

          @Override public void update(int value) {
            // do nothing
          }

          @Override public void update(long value) {
            // do nothing
          }
        };
      }

      @Override public long count() {
        return 0;
      }

      @Override public void update(int value) {
        // do nothing
      }

      @Override public void update(long value) {
        // do nothing
      }
    };
  }

  @Override public Timer registerTimer(String name, String help, String... tagNames) {
    return new Timer() {
      @Override public Child tags(String... tagValues) {
        return new Child() {
          @Override public long count() {
            return 0;
          }

          @Override public Context startTimer() {
            return () -> 0;
          }

          @Override public void update(long duration, TimeUnit unit) {
            // do nothing
          }
        };
      }

      @Override public long count() {
        return 0;
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
    return '_';
  }
}
