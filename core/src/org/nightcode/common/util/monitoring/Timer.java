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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A timer metric which aggregates timing durations.
 */
@Beta
public interface Timer extends Metric {

  /**
   * Returns a new {@link Context}.
   */
  Context startTimer();

  /**
   * Updates the timer with the difference between current and start time.
   */
  void update(long duration, TimeUnit unit);


  /**
   * Executes callable code.
   */
  default <E> E time(Callable<E> event) throws Exception {
    Context context = startTimer();
    try {
      return event.call();
    } finally {
      context.stop();
    }
  }

  /**
   * Executes runnable code.
   */
  default void time(Runnable event) {
    Context context = startTimer();
    try {
      event.run();
    } finally {
      context.stop();
    }
  }
}
