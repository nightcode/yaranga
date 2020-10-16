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
import org.nightcode.common.util.monitoring.CollectorHolder;
import org.nightcode.common.util.monitoring.MonitoringContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Beta
abstract class AbstractCollectorHolder<T extends Collector> implements CollectorHolder {

  protected final T target;
  protected final MonitoringContext context;

  protected final Map<List<String>, Collector> children = new HashMap<>();

  AbstractCollectorHolder(T target, MonitoringContext context) {
    this.target = target;
    this.context = context;
  }

  @Override public void addChild(Collector child, String... tagValues) {
    children.put(Arrays.asList(tagValues), child);
  }

  @Override public Collection<Collector> children() {
    return children.values();
  }

  @Override public Collector getChild(String... tagValues) {
    return children.get(Arrays.asList(tagValues));
  }

  @Override public CollectorName name() {
    return target.name();
  }

  @Override public Collector target() {
    return target;
  }
}
