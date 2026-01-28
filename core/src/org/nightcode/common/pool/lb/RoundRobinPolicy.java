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

package org.nightcode.common.pool.lb;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.nightcode.common.base.AbstractIterator;
import org.nightcode.common.pool.metadata.Endpoint;

/**
 * Round-robin strategy for load balancing.
 */
public class RoundRobinPolicy implements LoadBalancingPolicy {

  private final CopyOnWriteArrayList<Endpoint<?>> live  = new CopyOnWriteArrayList<>();
  private final AtomicInteger                     index = new AtomicInteger();

  public RoundRobinPolicy() {
    this.index.set(0);
  }

  @Override public int healthyCount() {
    return live.size();
  }

  @Override public <A> Iterator<Endpoint<A>> sessions() {
    // noinspection unchecked
    final List<Endpoint<A>> targets  = (List<Endpoint<A>>) live.clone();
    final int               startIdx = index.getAndIncrement();

    if (startIdx > Integer.MAX_VALUE - 10000) {
      index.set(0);
    }

    return new AbstractIterator<>() {
      private int idx       = startIdx;
      private int remaining = targets.size();

      @Override protected Endpoint<A> computeNext() {
        if (remaining <= 0) {
          return endOfData();
        }

        remaining--;
        int c = idx++ % targets.size();
        if (c < 0) {
          c += targets.size();
        }
        return targets.get(c);
      }
    };
  }

  @Override public void onDeregister(Endpoint<?> endpoint) {
    live.remove(endpoint);
  }

  @Override public void onRegister(Endpoint<?> endpoint) {
    live.addIfAbsent(endpoint);
  }
}
