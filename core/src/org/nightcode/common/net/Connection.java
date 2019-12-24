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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

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
    NEW(0x00000000),
    STARTING(0x00000001),
    ACTIVE(0x00000002),
    SHUTDOWN(0x00000004),
    STOPPING(0x00000008),
    IDLE(0x00000010),
    CLOSED(0x00000020);

    private final int state;

    State(int state) {
      this.state = state;
    }

    public int state() {
      return state;
    }
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

  protected final AtomicReference<State> state = new AtomicReference<>(State.NEW);

  private final Set<EventListener<Connection<A>, State>> listeners = new CopyOnWriteArraySet<>();

  public Connection(String name, A address) {
    this.name = name;
    this.address = address;
  }

  public boolean addEventListener(EventListener<Connection<A>, State> listener) {
    return listeners.add(listener);
  }

  public A address() {
    return address;
  }

  public State getState() {
    return state.get();
  }

  public String name() {
    return name;
  }

  public abstract void open() throws IOException;

  public boolean removeEventListener(EventListener<Connection<A>, State> listener) {
    return listeners.remove(listener);
  }

  protected void fireEvent(Event<Connection<A>, State> event) {
    for (EventListener<Connection<A>, State> listener : listeners) {
      listener.onEvent(event);
    }
  }

  protected void fireStateEvent(State state) {
    fireEvent(new ConnectionEvent<>(this, state));
  }
}
