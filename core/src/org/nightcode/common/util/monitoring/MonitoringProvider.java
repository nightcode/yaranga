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

import java.util.function.Supplier;

/**
 * Interface to provide metric's implementations.
 */
@Beta
public interface MonitoringProvider {

  boolean deregister(Collector metric);

  Counter createCounter(CollectorName name);

  Gauge createGauge(CollectorName name);

  <V> Gauge createGauge(CollectorName name, Supplier<V> gauge);

  Histogram createHistogram(CollectorName name);

  Timer createTimer(CollectorName name);

  String name();

  char nameSeparator();
}
