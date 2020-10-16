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
import org.nightcode.common.util.logging.LogManager;
import org.nightcode.common.util.logging.Logger;
import org.nightcode.common.util.monitoring.Counter;
import org.nightcode.common.util.monitoring.Gauge;
import org.nightcode.common.util.monitoring.Histogram;
import org.nightcode.common.util.monitoring.MonitoringEngine;
import org.nightcode.common.util.monitoring.MonitoringProvider;
import org.nightcode.common.util.monitoring.Timer;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * Base class for monitoring.
 */
@Beta
public final class MonitoringManager {

  private static final Logger LOGGER = LogManager.getLogger(MonitoringManager.class);

  private static final MonitoringManager INSTANCE;

  static {
    MonitoringEngine engine = null;
    String cname = null;
    try {
      cname = System.getProperty("yaranga.monitoring.engine");
      if (cname != null) {
        try {
          Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(cname);
          engine = (MonitoringEngine) clazz.newInstance();
        } catch (ClassNotFoundException ex) {
          Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(cname);
          engine = (MonitoringEngine) clazz.newInstance();
        }
      }
    } catch (Exception ex) {
      LOGGER.error(ex, "unable to load MonitoringEngine '%s'", cname);
    }
    if (engine == null) {
      engine = new NullMonitoringEngine();
    }

    MonitoringProvider provider = null;
    cname = null;
    try {
      cname = System.getProperty("yaranga.monitoring.provider");
      if (cname != null) {
        try {
          Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(cname);
          Constructor<?> constructor = clazz.getConstructor(MonitoringEngine.class);
          provider = (MonitoringProvider) constructor.newInstance(new Object[] {engine});
        } catch (ClassNotFoundException ex) {
          Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(cname);
          Constructor<?> constructor = clazz.getConstructor(MonitoringEngine.class);
          provider = (MonitoringProvider) constructor.newInstance(new Object[] {engine});
        }
      }
    } catch (Exception ex) {
      LOGGER.error(ex, "unable to load MonitoringProvider '%s'", cname);
    }
    if (provider == null) {
      provider = new DefaultMonitoringProvider(engine);
    }

    String subsystem = System.getProperty("yaranga.monitoring.subsystem");
    INSTANCE = new MonitoringManager(subsystem, provider);
    LOGGER.config("MonitoringManager has been initialized with provider '%s' and engine '%s'"
        , provider.getClass().getName(), engine.getClass().getName());
  }

  public static Counter counter(String name) {
    return INSTANCE.counterImpl(name);
  }

  public static Counter counter(String name, String... tagNames) {
    return INSTANCE.counterImpl(name, tagNames);
  }

  public static boolean deregister(String name) {
    return INSTANCE.deregisterImpl(name);
  }

  public static Gauge gauge(String name, Supplier<?> gauge) {
    return INSTANCE.gaugeImpl(name, gauge);
  }

  public static Gauge gauge(String name, String... tagNames) {
    return INSTANCE.gaugeImpl(name, tagNames);
  }

  public static Histogram histogram(String name) {
    return INSTANCE.histogramImpl(name);
  }

  public static Histogram histogram(String name, String... tagNames) {
    return INSTANCE.histogramImpl(name, tagNames);
  }

  public static char nameSeparator() {
    return INSTANCE.provider.engine().nameSeparator();
  }

  public static Counter registerCounter(String name) {
    return INSTANCE.registerCounterImpl(name);
  }

  public static Counter registerCounter(String name, String... tagNames) {
    return INSTANCE.registerCounterImpl(name, tagNames);
  }

  public static Gauge registerGauge(String name, Supplier<?> gauge) {
    return INSTANCE.registerGaugeImpl(name, gauge);
  }

  public static Gauge registerGauge(String name, String... tagNames) {
    return INSTANCE.registerGaugeImpl(name, tagNames);
  }

  public static Histogram registerHistogram(String name) {
    return INSTANCE.registerHistogramImpl(name);
  }

  public static Histogram registerHistogram(String name, String... tagNames) {
    return INSTANCE.registerHistogramImpl(name, tagNames);
  }

  public static Timer registerTimer(String name) {
    return INSTANCE.registerTimerImpl(name);
  }

  public static Timer registerTimer(String name, String... tagNames) {
    return INSTANCE.registerTimerImpl(name, tagNames);
  }

  public static String subsystem() {
    return INSTANCE.subsystem;
  }

  public static Timer timer(String name) {
    return INSTANCE.timerImpl(name);
  }

  public static Timer timer(String name, String... tagNames) {
    return INSTANCE.timerImpl(name, tagNames);
  }

  private final String subsystem;
  private final MonitoringProvider provider;

  private MonitoringManager(String subsystem, MonitoringProvider provider) {
    if (subsystem == null) {
      subsystem = "";
    }
    this.subsystem = subsystem;
    this.provider = provider;
  }

  private Counter counterImpl(String name) {
    return (Counter) provider.computeIfAbsent(CollectorName.build(name), CollectorType.COUNTER);
  }

  private Counter counterImpl(String name, String... tagNames) {
    return (Counter) provider.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.COUNTER);
  }

  private boolean deregisterImpl(String name) {
    return provider.deregister(CollectorName.build(name));
  }

  private Gauge gaugeImpl(String name, Supplier<?> gauge) {
    return (Gauge) provider.computeIfAbsentGauge(CollectorName.build(name), gauge);
  }

  private Gauge gaugeImpl(String name, String... tagNames) {
    return (Gauge) provider.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.GAUGE);
  }

  private Histogram histogramImpl(String name) {
    return (Histogram) provider.computeIfAbsent(CollectorName.build(name), CollectorType.HISTOGRAM);
  }

  private Histogram histogramImpl(String name, String... tagNames) {
    return (Histogram) provider.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.HISTOGRAM);
  }

  private Counter registerCounterImpl(String name) {
    return (Counter) provider.register(CollectorName.build(name), CollectorType.COUNTER);
  }

  private Counter registerCounterImpl(String name, String... tagNames) {
    return (Counter) provider.register(CollectorName.build(name, tagNames), CollectorType.COUNTER);
  }

  private Gauge registerGaugeImpl(String name, Supplier<?> gauge) {
    return (Gauge) provider.registerGauge(CollectorName.build(name), gauge);
  }

  private Gauge registerGaugeImpl(String name, String... tagNames) {
    return (Gauge) provider.register(CollectorName.build(name, tagNames), CollectorType.GAUGE);
  }

  private Histogram registerHistogramImpl(String name) {
    return (Histogram) provider.register(CollectorName.build(name), CollectorType.HISTOGRAM);
  }

  private Histogram registerHistogramImpl(String name, String... tagNames) {
    return (Histogram) provider.register(CollectorName.build(name, tagNames), CollectorType.HISTOGRAM);
  }

  private Timer registerTimerImpl(String name) {
    return (Timer) provider.register(CollectorName.build(name), CollectorType.TIMER);
  }

  private Timer registerTimerImpl(String name, String... tagNames) {
    return (Timer) provider.register(CollectorName.build(name, tagNames), CollectorType.TIMER);
  }

  private Timer timerImpl(String name) {
    return (Timer) provider.computeIfAbsent(CollectorName.build(name), CollectorType.TIMER);
  }

  private Timer timerImpl(String name, String... tagNames) {
    return (Timer) provider.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.TIMER);
  }
}
