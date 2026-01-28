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

import org.nightcode.common.pool.metadata.Endpoint;

/**
 * Base load balancing policy interface.
 */
public interface LoadBalancingPolicy {

  static LoadBalancingPolicy def() {
    return new RoundRobinPolicy();
  }

  void onDeregister(Endpoint<?> endpoint);

  int healthyCount();

  void onRegister(Endpoint<?> endpoint);

  <A> Iterator<Endpoint<A>> sessions();
}
