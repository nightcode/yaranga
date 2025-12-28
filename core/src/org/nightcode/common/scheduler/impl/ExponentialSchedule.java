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
 * An exponential schedule.
 */
public class ExponentialSchedule implements RetrySchedule {

  private final long minDelayMs;
  private final long maxDelayMs;
  private final long maxAttempts;

  public ExponentialSchedule(long minDelayMs, long maxDelayMs) {
    if (minDelayMs <= 0L) {
      throw new IllegalArgumentException("minDelayMs must be strictly positive");
    }
    if (maxDelayMs < minDelayMs) {
      throw new IllegalArgumentException(format("maxDelayMs [%d] cannot be smaller than minDelayMs [%d]", maxDelayMs, minDelayMs));
    }

    this.minDelayMs = minDelayMs;
    this.maxDelayMs = maxDelayMs;

    long ceil = (minDelayMs & (minDelayMs - 1L)) == 0 ? 0L : 1L;
    this.maxAttempts = 64L - Long.numberOfLeadingZeros(Long.MAX_VALUE / minDelayMs) - ceil;
  }

  @Override public long nextDelayMs(long attempt) {
    if (attempt > maxAttempts) {
      return maxDelayMs;
    }
    return (long) Math.min(minDelayMs * Math.pow(Math.E, 0.5 * attempt), maxDelayMs);
  }
}
