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

public class RetryConfig {

  private BackoffFunction backoffFunction;
  private long            minDelayMs;
  private long            maxDelayMs;
  private long            maxAttempts;

  public BackoffFunction getBackoffFunction() {
    return backoffFunction;
  }

  public long getMaxAttempts() {
    return maxAttempts;
  }

  public long getMaxDelayMs() {
    return maxDelayMs;
  }

  public long getMinDelayMs() {
    return minDelayMs;
  }

  public void setMaxAttempts(long maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public void setBackoffFunction(BackoffFunction backoffFunction) {
    this.backoffFunction = backoffFunction;
  }

  public void setMaxDelayMs(long maxDelayMs) {
    this.maxDelayMs = maxDelayMs;
  }

  public void setMinDelayMs(long minDelayMs) {
    this.minDelayMs = minDelayMs;
  }

  @Override public String toString() {
    return "RetryConfig{"
        + "backoffFunction=" + backoffFunction
        + ", minDelayMs=" + minDelayMs
        + ", maxDelayMs=" + maxDelayMs
        + ", maxAttempts=" + maxAttempts
        + '}';
  }
}
