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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link BackoffFunction}.
 */
public class BackoffFunctionTest {

  @Test public void linearSchedule() {
    RetrySchedule schedule = BackoffFunction.LINEAR.newSchedule(new RetryConfig(10, 0));

    Assert.assertEquals(10, schedule.nextDelayMs(0));
    Assert.assertEquals(10, schedule.nextDelayMs(1));
    Assert.assertEquals(10, schedule.nextDelayMs(2));
    Assert.assertEquals(10, schedule.nextDelayMs(3));
    Assert.assertEquals(10, schedule.nextDelayMs(4));
  }

  @Test public void arithmeticSchedule() {
    RetrySchedule schedule = BackoffFunction.ARITHMETIC.newSchedule(new RetryConfig(1, 4));

    Assert.assertEquals(1, schedule.nextDelayMs(0));
    Assert.assertEquals(2, schedule.nextDelayMs(1));
    Assert.assertEquals(3, schedule.nextDelayMs(2));
    Assert.assertEquals(4, schedule.nextDelayMs(3));
    Assert.assertEquals(4, schedule.nextDelayMs(4));
    Assert.assertEquals(4, schedule.nextDelayMs(5));
  }

  @Test public void geometricSchedule() {
    RetrySchedule schedule = BackoffFunction.GEOMETRIC.newSchedule(new RetryConfig(1, 20));

    Assert.assertEquals(1, schedule.nextDelayMs(0));
    Assert.assertEquals(2, schedule.nextDelayMs(1));
    Assert.assertEquals(4, schedule.nextDelayMs(2));
    Assert.assertEquals(8, schedule.nextDelayMs(3));
    Assert.assertEquals(16, schedule.nextDelayMs(4));
    Assert.assertEquals(20, schedule.nextDelayMs(5));
    Assert.assertEquals(20, schedule.nextDelayMs(6));
  }

  @Test public void exponentialSchedule() {
    RetrySchedule schedule = BackoffFunction.EXPONENTIAL.newSchedule(new RetryConfig(1, 50));

    Assert.assertEquals(1, schedule.nextDelayMs(0));
    Assert.assertEquals(1, schedule.nextDelayMs(1));
    Assert.assertEquals(2, schedule.nextDelayMs(2));
    Assert.assertEquals(4, schedule.nextDelayMs(3));
    Assert.assertEquals(7, schedule.nextDelayMs(4));
    Assert.assertEquals(12, schedule.nextDelayMs(5));
    Assert.assertEquals(20, schedule.nextDelayMs(6));
    Assert.assertEquals(33, schedule.nextDelayMs(7));
    Assert.assertEquals(50, schedule.nextDelayMs(8));
    Assert.assertEquals(50, schedule.nextDelayMs(9));
  }
}
