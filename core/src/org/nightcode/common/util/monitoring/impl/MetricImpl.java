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
import org.nightcode.common.util.monitoring.Collector;
import org.nightcode.common.util.monitoring.Gauge;
import org.nightcode.common.util.monitoring.Metric;
import org.nightcode.common.util.monitoring.MonitoringProvider;

import java.util.function.Supplier;

import javax.annotation.Nullable;

@Beta
class MetricImpl implements Metric {

  private final CollectorName name;
  private final CollectorType type;
  private final MonitoringManager mm;

  private CollectorHolder collector;

  MetricImpl(CollectorName name, @Nullable CollectorHolder collector, CollectorType type, MonitoringManager mm) {
    this.name = name;
    this.collector = collector;
    this.type = type;
    this.mm = mm;
  }

  @Override public Collector collector() {
    return collector;
  }

  @Override public void doDeregister() {
    mm.metrics().remove(name);
    MonitoringProvider provider = mm.provider();
    for (Collector child : collector.children()) {
      provider.deregister(child);
    }
    collector.children().clear();
    provider.deregister(collector);
  }

  @Override public void doRegister() {
    collector = type.create(name, mm);
    mm.metrics().put(name, collector);
  }

  @Override public void doRegisterGauge(Supplier<?> gauge) {
    collector = new GaugeHolder(mm.provider().createGauge(name, gauge), mm);
    mm.metrics().put(name, collector);
  }

  @Override public void doTags(String... tagValues) {
    if (collector.getChild(tagValues) == null) {
      Collector child = type.createChild(collector.target(), tagValues);
      collector.addChild(child, tagValues);
    }
  }

  @Override public void doTagsGauge(Supplier<?> gauge, String... tagValues) {
    if (collector.getChild(tagValues) == null) {
      Gauge.Child child = ((Gauge) collector.target()).tags(gauge, tagValues);
      collector.addChild(child, tagValues);
    }
  }

  @Override public Collector tags(String... tagValues) {
    return collector.getChild(tagValues);
  }
}
