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

package org.nightcode.common.net;

import org.nightcode.common.util.event.Event;
import org.nightcode.common.util.event.EventListener;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A general connections object.
 *
 * @param <A> the connection address
 */
public abstract class Connection<A> implements Closeable {

  /**
   * Connection's states.
   */
  public enum State {
    NEW,
    ACTIVE,
    CLOSED,
    INACTIVE,
    OPENED
  }

  /**
   * @param <A> the connection address
   */
  public static class ConnectionEvent<A> implements Event<Connection<A>, State> {

    private final Connection<A> connection;
    private final State type;

    public ConnectionEvent(Connection<A> connection, State type) {
      this.connection = connection;
      this.type = type;
    }

    @Override public Connection<A> subject() {
      return connection;
    }

    @Override public State type() {
      return type;
    }
  }

  private final String name;
  private final A address;
  private volatile State state = State.NEW;

  private final Set<EventListener<ConnectionEvent<A>>> eventListeners = new CopyOnWriteArraySet<>();

  public Connection(String name, A address) {
    this.name = name;
    this.address = address;
  }

  public void active() {
    state = State.ACTIVE;
    fireEvent(new ConnectionEvent<>(this, State.ACTIVE));
  }

  public boolean addEventListener(EventListener<ConnectionEvent<A>> listener) {
    return eventListeners.add(listener);
  }

  public A address() {
    return address;
  }

  @Override public final void close() throws IOException {
    doClose();
    state = State.CLOSED;
    fireEvent(new ConnectionEvent<>(this, State.CLOSED));
  }

  public abstract void doClose() throws IOException;

  public abstract void doOpen() throws IOException;

  public abstract <Q, R> CompletableFuture<R> executeAsync(Q request);

  public State getState() {
    return state;
  }

  public void inactive() {
    state = State.INACTIVE;
    fireEvent(new ConnectionEvent<>(this, State.INACTIVE));
  }

  public String name() {
    return name;
  }

  public final void open() throws IOException {
    doOpen();
    state = State.OPENED;
    fireEvent(new ConnectionEvent<>(this, State.OPENED));
  }

  public boolean removeEventListener(EventListener<ConnectionEvent<A>> listener) {
    return eventListeners.remove(listener);
  }

  private void fireEvent(ConnectionEvent<A> event) {
    for (EventListener<ConnectionEvent<A>> listener : eventListeners) {
      listener.onEvent(event);
    }
  }
}
