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
import org.nightcode.common.util.monitoring.Gauge;

import java.util.function.Supplier;

@Beta
class GaugeHolder extends AbstractCollectorHolder<Gauge> implements Gauge {

  GaugeHolder(Gauge target, MonitoringManager monitoringManager) {
    super(target, monitoringManager);
  }

  @Override public Gauge.Child tags(Supplier<?> gauge, String... tagValues) {
    return (Gauge.Child) monitoringManager.tags(name(), gauge, tagValues);
  }

  @Override public CollectorType type() {
    return CollectorType.GAUGE;
  }
}
