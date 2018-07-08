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
 *
 */
public class RoundRobinLoadBalancingPolicy implements LoadBalancingPolicy {

  private static final class ConnectionIterator implements Iterator<Connection> {

    private Connection next;
    private boolean ready;

    private int initIndex;
    private int remaining;

    private final List<Connection> connections;

    private ConnectionIterator(int initIndex, List<Connection> connections) {
      this.initIndex = initIndex;
      this.connections = connections;
      this.remaining = connections.size();
    }

    @Override public boolean hasNext() {
      return ready || tryNext();
    }

    @Override public Connection next() {
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

  private final CopyOnWriteArrayList<Connection> liveConnections = new CopyOnWriteArrayList<>();

  @Override public void init(Collection<Connection> connections) {
    for (Connection connection : connections) {
      connection.addStateListener(this);
    }
  }

  @Override public void onActive(Connection connection) {
    liveConnections.addIfAbsent(connection);
  }

  @Override public void onInactive(Connection connection) {
    liveConnections.remove(connection);
  }

  @Override public Iterator<Connection> selectConnections() {
    int initIndex = index.getAndIncrement();
    if (initIndex > INDEX_THRESHOLD) {
      index.set(0);
    }
    @SuppressWarnings("unchecked")
    List<Connection> live = (List<Connection>) liveConnections.clone();
    return new ConnectionIterator(initIndex, live);
  }
}
