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
 * An incrementing and decrementing counter collector.
 */
@Beta
public interface Counter extends Collector {

  /**
   * An interface for Counter's child.
   */
  interface Child extends Collector {

    /**
     * Increment the counter by 1.
     */
    void inc();

    /**
     * Increment the counter by {@code value}.
     *
     * @param value the amount by which the counter will be increased
     */
    void inc(long value);

    /**
     * Decrement the counter by one.
     */
    void dec();

    /**
     * Decrement the counter by {@code value}.
     *
     * @param value the amount by which the counter will be decreased
     */
    void dec(long value);

    /**
     * Get the value of the counter.
     *
     * @return the value of the counter
     */
    long getCount();
  }

  /**
   * Increment the counter by 1.
   */
  void inc();

  /**
   * Increment the counter by {@code value}.
   *
   * @param value the amount by which the counter will be increased
   */
  void inc(long value);

  /**
   * Decrement the counter by one.
   */
  void dec();

  /**
   * Decrement the counter by {@code value}.
   *
   * @param value the amount by which the counter will be decreased
   */
  void dec(long value);

  /**
   * Get the value of the counter.
   *
   * @return the value of the counter
   */
  long getCount();

  /**
   * Set tag values.
   *
   * @param tagValues tag values
   * @return counter
   */
  Child tags(String... tagValues);
}
