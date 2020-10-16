package org.nightcode.common.util.monitoring.impl;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.util.monitoring.Collector;
import org.nightcode.common.util.monitoring.CollectorHolder;
import org.nightcode.common.util.monitoring.Metric;
import org.nightcode.common.util.monitoring.MonitoringContext;
import org.nightcode.common.util.monitoring.MonitoringEngine;
import org.nightcode.common.util.monitoring.MonitoringProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Default implementation of the MonitoringProvider interface.
 */
@Beta
final class DefaultMonitoringProvider implements MonitoringProvider {

  private final MonitoringEngine engine;
  private final Map<CollectorName, CollectorHolder> metrics;

  private final ReentrantLock lock = new ReentrantLock();

  DefaultMonitoringProvider(MonitoringEngine engine) {
    this.engine = engine;
    this.metrics = new HashMap<>();
  }

  private MonitoringContext context(CollectorName name) {
    return new MonitoringContextImpl(name, lock, metrics, this);
  }

  @Override public Collector computeIfAbsent(CollectorName name, CollectorType type) {
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

  @Override public Collector computeIfAbsentGauge(CollectorName name, Supplier<?> gauge) {
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

  @Override public boolean deregister(CollectorName name) {
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

  @Override public MonitoringEngine engine() {
    return engine;
  }

  @Override public Collector register(CollectorName name, CollectorType type) {
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

  @Override public Collector registerGauge(CollectorName name,  Supplier<?> gauge) {
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

  @Override public Collector tags(CollectorName name, String... tagValues) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric == null) {
        throw new IllegalArgumentException("a collector with name " + name + " does not exist");
      }
      if (tagValues.length != name.tagNames().length) {
        throw new IllegalArgumentException("incorrect number of tags");
      }
      context.registerCollectorTags(metric, tagValues);
      return metric.tags(tagValues);
    } finally {
      context.unlock();
    }
  }

  @Override public Collector tags(CollectorName name, Supplier<?> gauge, String... tagValues) {
    MonitoringContext context = context(name);
    context.lock();
    try {
      Metric metric = context.metric();
      if (metric == null) {
        throw new IllegalArgumentException("a collector with name " + name + " does not exist");
      }
      if (tagValues.length != name.tagNames().length) {
        throw new IllegalArgumentException("incorrect number of tags");
      }
      context.registerGaugeTags(metric, gauge, tagValues);
      return metric.tags(tagValues);
    } finally {
      context.unlock();
    }
  }
}
