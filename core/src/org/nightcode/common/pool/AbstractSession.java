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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.util.Clock;

/**
 * Absract session implementation.
 *
 * @param <A> the session address
 */
@Beta
public abstract class AbstractSession<A> implements Session<A> {

  protected final Endpoint<A> endpoint;

  protected final Clock clock = Clock.sys();

  protected final AtomicReference<State> state = new AtomicReference<>(State.NEW);

  private final Set<StateListener<A>> listeners = new CopyOnWriteArraySet<>();

  protected AbstractSession(Endpoint<A> endpoint) {
    this.endpoint = endpoint;
  }

  @Override public void addListener(StateListener<A> listener) {
    listeners.add(listener);
  }

  @Override public void destroy() {
    if (State.DESTROYED == state.get()) {
      return;
    }

    fireStateEvent(State.DESTROYING);

    tryCancelScheduledFutures();

    destroyImpl();

    state.set(State.DESTROYED);
    fireStateEvent(State.DESTROYED);
  }

  @Override public Endpoint<A> endpoint() {
    return endpoint;
  }

  public State state() {
    return state.get();
  }

  @Override public Set<StateListener<A>> listeners() {
    return listeners;
  }

  @Override public void initialize() {
    if (!state.compareAndSet(State.NEW, State.IDLE)) {
      return;
    }
    createImpl();
  }

  @Override public void removeAllListeners() {
    listeners.clear();
  }

  @Override public void removeListener(StateListener<A> listener) {
    listeners.remove(listener);
  }

  @Override public String toString() {
    return endpoint.toString();
  }

  protected abstract void createImpl();

  protected abstract void destroyImpl();

  protected void tryCancelScheduledFutures() {
    // do nothing
  }
}
