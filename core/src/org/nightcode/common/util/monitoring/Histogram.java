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

/**
 * Histogram collector, to track distributions of events.
 */
@Beta
public interface Histogram extends Collector {

  /**
   * An interface for Collector's child.
   */
  interface Child extends Collector {

    long count();

    void update(int value);

    void update(long value);
  }

  long count();

  /**
   * Set tag values.
   *
   * @param tagValues tag values
   * @return histogram
   */
  Child tags(String... tagValues);

  void update(int value);

  void update(long value);
}
