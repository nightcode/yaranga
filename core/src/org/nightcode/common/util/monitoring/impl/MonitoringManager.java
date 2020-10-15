package org.nightcode.common.util.monitoring.impl;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.util.logging.LogManager;
import org.nightcode.common.util.logging.Logger;
import org.nightcode.common.util.monitoring.Collector;
import org.nightcode.common.util.monitoring.Counter;
import org.nightcode.common.util.monitoring.Gauge;
import org.nightcode.common.util.monitoring.Histogram;
import org.nightcode.common.util.monitoring.Metric;
import org.nightcode.common.util.monitoring.MonitoringContext;
import org.nightcode.common.util.monitoring.MonitoringProvider;
import org.nightcode.common.util.monitoring.Timer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Base class for monitoring.
 */
@Beta
public final class MonitoringManager {

  private static final MonitoringManager INSTANCE;

  private static final Logger LOGGER = LogManager.getLogger(MonitoringManager.class);

  static {
    MonitoringProvider provider = null;
    String cname = null;
    try {
      cname = System.getProperty("yaranga.monitoring.provider");
      if (cname != null) {
        try {
          Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(cname);
          provider = (MonitoringProvider) clazz.newInstance();
        } catch (ClassNotFoundException ex) {
          Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(cname);
          provider = (MonitoringProvider) clazz.newInstance();
        }
      }
    } catch (Exception ex) {
      LOGGER.error(ex, "unable to load MonitoringProvider '%s'", cname);
    }

    if (provider == null) {
      provider = new NullMonitoringProvider();
    }

    String subsystem = System.getProperty("yaranga.monitoring.subsystem");
    INSTANCE = new MonitoringManager(subsystem, provider);
    LOGGER.config("MonitoringManager has been initialized with provider '%s'", provider.name());
  }

  public static String subsystem() {
    return INSTANCE.subsystem;
  }

  public static Counter counter(String name) {
    return (Counter) INSTANCE.computeIfAbsent(CollectorName.build(name), CollectorType.COUNTER);
  }

  public static Counter counter(String name, String... tagNames) {
    return (Counter) INSTANCE.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.COUNTER);
  }

  public static boolean deregister(String name) {
    return INSTANCE.deregister(CollectorName.build(name));
  }

  public static <V> Gauge gauge(String name, Supplier<V> gauge) {
    return (Gauge) INSTANCE.computeIfAbsentGauge(CollectorName.build(name), gauge);
  }

  public static Gauge gauge(String name, String... tagNames) {
    return (Gauge) INSTANCE.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.GAUGE);
  }

  public static Histogram histogram(String name) {
    return (Histogram) INSTANCE.computeIfAbsent(CollectorName.build(name), CollectorType.HISTOGRAM);
  }

  public static Histogram histogram(String name, String... tagNames) {
    return (Histogram) INSTANCE.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.HISTOGRAM);
  }

  public static Counter registerCounter(String name) {
    return (Counter) INSTANCE.register(CollectorName.build(name), CollectorType.COUNTER);
  }

  public static Counter registerCounter(String name, String... tagNames) {
    return (Counter) INSTANCE.register(CollectorName.build(name, tagNames), CollectorType.COUNTER);
  }

  public static <V> Gauge registerGauge(String name, Supplier<V> gauge) {
    return (Gauge) INSTANCE.registerGauge(CollectorName.build(name), gauge);
  }

  public static Gauge registerGauge(String name, String... tagNames) {
    return (Gauge) INSTANCE.register(CollectorName.build(name, tagNames), CollectorType.GAUGE);
  }

  public static Histogram registerHistogram(String name) {
    return (Histogram) INSTANCE.register(CollectorName.build(name), CollectorType.HISTOGRAM);
  }

  public static Histogram registerHistogram(String name, String... tagNames) {
    return (Histogram) INSTANCE.register(CollectorName.build(name, tagNames), CollectorType.HISTOGRAM);
  }

  public static Timer registerTimer(String name) {
    return (Timer) INSTANCE.register(CollectorName.build(name), CollectorType.TIMER);
  }

  public static Timer registerTimer(String name, String... tagNames) {
    return (Timer) INSTANCE.register(CollectorName.build(name, tagNames), CollectorType.TIMER);
  }

  public static Timer timer(String name) {
    return (Timer) INSTANCE.computeIfAbsent(CollectorName.build(name), CollectorType.TIMER);
  }

  public static Timer timer(String name, String... tagNames) {
    return (Timer) INSTANCE.computeIfAbsent(CollectorName.build(name, tagNames), CollectorType.TIMER);
  }

  static char nameSeparator() {
    return INSTANCE.provider.nameSeparator();
  }

  private final String subsystem;
  private final MonitoringProvider provider;

  private final Map<CollectorName, CollectorHolder> metrics;

  private final ReentrantLock lock = new ReentrantLock();

  private MonitoringManager(@Nullable String subsystem, MonitoringProvider provider) {
    this.provider = provider;
    if (subsystem == null) {
      subsystem = "";
    }
    this.subsystem = subsystem;
    this.metrics = new HashMap<>();
  }

  void lock() {
    lock.lock();
  }

  Map<CollectorName, CollectorHolder> metrics() {
    return metrics;
  }

  MonitoringProvider provider() {
    return provider;
  }

  Collector tags(CollectorName name, String... tagValues) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric == null) {
        throw new IllegalArgumentException("a collector with name " + name + " does not exist");
      }
      context.registerCollectorTags(metric, tagValues);
      return metric.tags(tagValues);
    } finally {
      context.unlock();
    }
  }

  Collector tags(CollectorName name, Supplier<?> gauge, String... tagValues) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric == null) {
        throw new IllegalArgumentException("a collector with name " + name + " does not exist");
      }
      context.registerGaugeTags(metric, gauge, tagValues);
      return metric.tags(tagValues);
    } finally {
      context.unlock();
    }
  }

  void unlock() {
    lock.unlock();
  }

  private MonitoringContext context(CollectorName name) {
    return new MonitoringContextImpl(name, this);
  }

  private Collector computeIfAbsent(CollectorName name, CollectorType type) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric != null) {
        Collector collector = metric.collector();
        if (!(type.collectorClass().isAssignableFrom(collector.getClass()))) {
          throw new IllegalArgumentException("collector with such name '"
              + name + "' and different type is already in use");
        }
        return collector;
      }
      metric = context.createMetric(type);
      context.registerCollector(metric);
      return metric.collector();
    } finally {
      context.unlock();
    }
  }

  private Collector computeIfAbsentGauge(CollectorName name, Supplier<?> gauge) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric != null) {
        Collector collector = metric.collector();
        if (!(CollectorType.GAUGE.collectorClass().isAssignableFrom(collector.getClass()))) {
          throw new IllegalArgumentException("collector with such name '"
              + name + "' and different type is already in use");
        }
        return collector;
      }
      metric = context.createMetric(CollectorType.GAUGE);
      context.registerGauge(metric, gauge);
      return metric.collector();
    } finally {
      context.unlock();
    }
  }

  private boolean deregister(CollectorName name) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric == null) {
        return false;
      }
      context.deregisterCollector(metric);
      return true;
    } finally {
      context.unlock();
    }
  }

  private Collector register(CollectorName name, CollectorType type) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric != null) {
        throw new IllegalArgumentException("a collector with name " + name + " already exists");
      }
      metric = context.createMetric(type);
      context.registerCollector(metric);
      return metric.collector();
    } finally {
      context.unlock();
    }
  }

  private Collector registerGauge(CollectorName name,  Supplier<?> gauge) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric != null) {
        throw new IllegalArgumentException("a collector with name " + name + " already exists");
      }
      metric = context.createMetric(CollectorType.GAUGE);
      context.registerGauge(metric, gauge);
      return metric.collector();
    } finally {
      context.unlock();
    }
  }
}
