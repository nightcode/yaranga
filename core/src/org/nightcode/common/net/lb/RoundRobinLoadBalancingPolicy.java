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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <A> the connection address
 */
public class RoundRobinLoadBalancingPolicy<A> implements LoadBalancingPolicy<A> {

  private static final class ConnectionIterator<A> implements Iterator<Connection<A>> {

    private Connection<A> next;
    private boolean ready;

    private int initIndex;
    private int remaining;

    private final List<Connection<A>> connections;

    private ConnectionIterator(int initIndex, List<Connection<A>> connections) {
      this.initIndex = initIndex;
      this.connections = connections;
      this.remaining = connections.size();
    }

    @Override public boolean hasNext() {
      return ready || tryNext();
    }

    @Override public Connection<A> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      } else {
        ready = false;
        return next;
      }
    }

    private boolean tryNext() {
      if (connections.isEmpty()) {
        return false;
      }
      if (remaining == 0) {
        return false;
      }
      int index = initIndex++ % connections.size();
      next = connections.get(index);
      ready = true;
      remaining--;
      return true;
    }
  }

  private static final int INDEX_THRESHOLD = Integer.MAX_VALUE >> 1;

  private final AtomicInteger index = new AtomicInteger(0);

  private final CopyOnWriteArrayList<Connection<A>> liveConnections = new CopyOnWriteArrayList<>();

  @Override public void init(Collection<Connection<A>> connections) {
    for (Connection<A> connection : connections) {
      connection.addEventListener(this);
    }
  }

  @Override public void onEvent(Connection.ConnectionEvent<A> event) {
    switch (event.type()) {
      case ACTIVE:
        liveConnections.addIfAbsent(event.subject());
        break;
      case INACTIVE:
        liveConnections.remove(event.subject());
        break;
      default:
        // do nothing
    }
  }

  @Override public Iterator<Connection<A>> selectConnections() {
    int initIndex = index.getAndIncrement();
    if (initIndex > INDEX_THRESHOLD) {
      index.set(0);
    }
    @SuppressWarnings("unchecked")
    List<Connection<A>> live = (List<Connection<A>>) liveConnections.clone();
    return new ConnectionIterator<>(initIndex, live);
  }
}
