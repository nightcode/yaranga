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
import org.nightcode.common.util.monitoring.Histogram;
import org.nightcode.common.util.monitoring.MonitoringContext;

@Beta
class HistogramHolder extends AbstractCollectorHolder<Histogram> implements Histogram {

  HistogramHolder(Histogram target, MonitoringContext context) {
    super(target, context);
  }

  @Override public Histogram.Child tags(String... tagValues) {
    return (Histogram.Child) context.provider().tags(name(), tagValues);
  }

  @Override public CollectorType type() {
    return CollectorType.HISTOGRAM;
  }

  @Override public void update(int value) {
    target.update(value);
  }

  @Override public void update(long value) {
    target.update(value);
  }
}
