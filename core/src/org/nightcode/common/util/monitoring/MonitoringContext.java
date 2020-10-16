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

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.util.monitoring.impl.CollectorName;
import org.nightcode.common.util.monitoring.impl.CollectorType;

import java.util.Map;

/**
 * A monitoring context.
 */
@Beta
public interface MonitoringContext extends MonitoringOperations {

  Metric createMetric(CollectorType type);

  MonitoringEngine engine();

  void lock();

  Metric metric();

  Map<CollectorName, CollectorHolder> metrics();

  MonitoringProvider provider();

  void unlock();
}
