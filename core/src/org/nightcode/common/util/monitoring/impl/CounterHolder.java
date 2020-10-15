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
import org.nightcode.common.util.monitoring.Counter;

@Beta
class CounterHolder extends AbstractCollectorHolder<Counter> implements Counter {

  CounterHolder(Counter target, MonitoringManager monitoringManager) {
    super(target, monitoringManager);
  }

  @Override public void inc() {
    target.inc();
  }

  @Override public void inc(long value) {
    target.inc(value);
  }

  @Override public void dec() {
    target.dec();
  }

  @Override public void dec(long value) {
    target.dec(value);
  }

  @Override public long getCount() {
    return target.getCount();
  }

  @Override public Counter.Child tags(String... tagValues) {
    return (Counter.Child) monitoringManager.tags(name(), tagValues);
  }

  @Override public CollectorType type() {
    return CollectorType.COUNTER;
  }
}
