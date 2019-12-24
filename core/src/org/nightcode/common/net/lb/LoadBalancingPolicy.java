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

package org.nightcode.common.net.lb;

import org.nightcode.common.net.Connection;
import org.nightcode.common.util.event.EventListener;

import java.util.Collection;
import java.util.Iterator;

/**
 * @param <A> the connection address
 * @param <C> the connection interface
 */
public interface LoadBalancingPolicy<A, C extends Connection<A>>
    extends EventListener<Connection<A>, Connection.State> {

  void addConnection(C connection);

  void addConnections(Collection<? extends C> connections);

  void removeConnection(C connection);

  Iterator<C> selectConnections();

  static <A, C extends Connection<A>> LoadBalancingPolicy<A, C> defaultLoadBalancingPolicy() {
    return new RoundRobinLoadBalancingPolicy<>();
  }
}
