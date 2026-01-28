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

package org.nightcode.common.pool;

import java.util.Set;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.lang.Event;
import org.nightcode.common.pool.metadata.Endpoint;

/**
 * A general pool interface.
 *
 * @param <A> the session address
 */
@Beta
public interface Session<A> {

  /**
   * Connection's states.
   */
  enum State {
    NEW(0x00),
    CREATING(0x01),
    ACTIVE(0x02),
    IDLE(0x04),
    TIMEOUT(0x08),
    DESTROYING(0x10),
    DESTROYED(0x20);

    private final int state;

    State(int state) {
      this.state = state;
    }

    public int state() {
      return state;
    }
  }

  interface StateListener<A> {
    void onEvent(Event<? extends Session<A>, State> event);
  }

  /**
   * @param <A> the session address
   * @param <S> the session
   */
  class SessionEvent<A, S extends Session<A>> implements Event<S, State> {

    private final S     session;
    private final State type;

    public SessionEvent(S session, State type) {
      this.session = session;
      this.type    = type;
    }

    @Override public S subject() {
      return session;
    }

    @Override public State type() {
      return type;
    }

    @Override public String toString() {
      return "SessionEvent{session=" + session + ", state=" + type + '}';
    }
  }

  void addListener(StateListener<A> listener);

  void destroy();

  Endpoint<A> endpoint();

  void initialize();

  Set<StateListener<A>> listeners();

  void removeListener(StateListener<A> listener);

  void removeAllListeners();

  default <S extends Session<A>> void fireEvent(SessionEvent<A, S> event) {
    for (StateListener<A> listener : listeners()) {
      listener.onEvent(event);
    }
  }

  default void fireStateEvent(State state) {
    fireEvent(new SessionEvent<>(this, state));
  }
}
