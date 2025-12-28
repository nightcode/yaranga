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

package org.nightcode.common.scheduler;

import org.nightcode.common.annotations.Beta;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * A general purpose interface for schedule.
 */
@Beta
public interface RetrySchedule {

  /**
   * Returns a delay in ms.
   *
   * @param attempt current iteration number
   * @return delay in ms
   */
  long nextDelayMs(long attempt);

  default LocalDateTime nextExecutionTime(long attempt) {
    LocalDateTime now = LocalDateTime.now();
    return now.plusSeconds(TimeUnit.MILLISECONDS.toSeconds(nextDelayMs(attempt)));
  }
}
