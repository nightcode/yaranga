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
import org.nightcode.common.util.monitoring.Timer;

import java.util.concurrent.TimeUnit;

@Beta
class TimerHolder extends AbstractCollectorHolder<Timer> implements Timer {

  TimerHolder(Timer target, MonitoringManager monitoringManager) {
    super(target, monitoringManager);
  }

  @Override public Timer.Child tags(String... tagValues) {
    return (Timer.Child) monitoringManager.tags(name(), tagValues);
  }

  @Override public Context startTimer() {
    return target.startTimer();
  }

  @Override public CollectorType type() {
    return CollectorType.TIMER;
  }

  @Override public void update(long duration, TimeUnit unit) {
    target.update(duration, unit);
  }
}
