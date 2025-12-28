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

package org.nightcode.common.scheduler.impl;

import org.nightcode.common.scheduler.RetrySchedule;

import static java.lang.String.format;

/**
 * A linear schedule.
 */
public class LinearSchedule implements RetrySchedule {

  private final long delayMs;

  public LinearSchedule(long delayMs) {
    if (delayMs < 0L) {
      throw new IllegalArgumentException(format("invalid negative delay (got %d)", delayMs));
    }
    this.delayMs = delayMs;
  }

  @Override public long nextDelayMs(long attempt) {
    return delayMs;
  }
}
