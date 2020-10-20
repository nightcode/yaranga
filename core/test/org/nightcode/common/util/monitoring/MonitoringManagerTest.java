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

import org.nightcode.common.util.monitoring.impl.MonitoringManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class MonitoringManagerTest {

  private static final String ERROR_TEMPLATE
      = "a collector with name CollectorName{name='%s', fullName='%s', tagNames=[]} already exists";

  @After public void treadDown() {
    MonitoringManager.deregister("testCounter");
    MonitoringManager.deregister("testGauge");
    MonitoringManager.deregister("testHistogram");
    MonitoringManager.deregister("testTimer");
  }

  @Test public void testRegister() {
    Counter counter1 = MonitoringManager.registerCounter("testCounter");
    Counter counter2 = MonitoringManager.counter("testCounter");
    Assert.assertEquals(counter1, counter2);
    try {
      MonitoringManager.registerCounter("testCounter");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals(String.format(ERROR_TEMPLATE, "testCounter", "testCounter"), ex.getMessage());
    }

    Gauge gauge1 = MonitoringManager.registerGauge("testGauge", () -> null);
    Gauge gauge2 = MonitoringManager.gauge("testGauge", () -> null);
    Assert.assertEquals(gauge1, gauge2);
    try {
      MonitoringManager.registerGauge("testGauge");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals(String.format(ERROR_TEMPLATE, "testGauge", "testGauge"), ex.getMessage());
    }

    Histogram histogram1 = MonitoringManager.registerHistogram("testHistogram");
    Histogram histogram2 = MonitoringManager.histogram("testHistogram");
    Assert.assertEquals(histogram1, histogram2);
    try {
      MonitoringManager.registerHistogram("testHistogram");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals(String.format(ERROR_TEMPLATE, "testHistogram", "testHistogram"), ex.getMessage());
    }

    Timer timer1 = MonitoringManager.registerTimer("testTimer");
    Timer timer2 = MonitoringManager.timer("testTimer");
    Assert.assertEquals(timer1, timer2);
    try {
      MonitoringManager.registerTimer("testTimer");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals(String.format(ERROR_TEMPLATE, "testTimer", "testTimer"), ex.getMessage());
    }
  }

  @Test public void testDeregister() {
    Counter counter1 = MonitoringManager.registerCounter("testCounter", "tag");
    MonitoringManager.deregister("testCounter");
    Counter counter2 = MonitoringManager.registerCounter("testCounter", "tag");
    Assert.assertNotEquals(counter1, counter2);

    Gauge gauge1 = MonitoringManager.registerGauge("testGauge", "tag");
    MonitoringManager.deregister("testGauge");
    Gauge gauge2 = MonitoringManager.registerGauge("testGauge", "tag");
    Assert.assertNotEquals(gauge1, gauge2);

    Histogram histogram1 = MonitoringManager.registerHistogram("testHistogram", "tag");
    MonitoringManager.deregister("testHistogram");
    Histogram histogram2 = MonitoringManager.registerHistogram("testHistogram", "tag");
    Assert.assertNotEquals(histogram1, histogram2);

    Timer timer1 = MonitoringManager.registerTimer("testTimer", "tag");
    MonitoringManager.deregister("testTimer");
    Timer timer2 = MonitoringManager.registerTimer("testTimer", "tag");
    Assert.assertNotEquals(timer1, timer2);
  }
}
