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

import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SimpleMessageQueue<T> implements MessageQueue<T> {

  private final Deque<T> queue;
  private final Consumer<T> consumer;

  private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

  public SimpleMessageQueue(Deque<T> queue, Consumer<T> consumer) {
    this.queue = queue;
    this.consumer = consumer;
  }

  @Override public void flush() {
    switch (state.get()) {
      case IDLE -> {
        if (state.compareAndSet(State.IDLE, State.FLUSH)) {
          flush0();
        }
      }
      case FLUSH -> state.compareAndSet(State.FLUSH, State.REFLUSH);
      case REFLUSH, INTERRUPT -> { }
      default -> throw new IllegalStateException("should not happen");
    }
  }

  @Override public int size() {
    return queue.size();
  }

  @Override public State state() {
    return state.get();
  }

  @Override public void resume() {
    if (state.compareAndSet(State.INTERRUPT, State.FLUSH)) {
      flush0();
    } else {
      flush();
    }
  }

  @Override public void tryInterrupt() {
    State s = state.get();
    switch (s) {
      case INTERRUPT -> { }
      case IDLE, FLUSH, REFLUSH -> state.compareAndSet(s, State.INTERRUPT);
      default -> throw new IllegalStateException("should not happen");
    }
  }

  @Override public void put(T message) {
    queue.addLast(message);
  }

  @Override public void putAndFlush(T message) {
    queue.addLast(message);
    flush();
  }

  private void flush0() {
    T next = queue.poll();
    while (true) {
      while (next != null) {
        consumer.accept(next);
        if (State.INTERRUPT == state.get()) {
          return;
        }
        next = queue.poll();
      }
      switch (state.get()) {
        case FLUSH -> {
          if (state.compareAndSet(State.FLUSH, State.IDLE)) {
            return;
          }
        }
        case REFLUSH -> state.compareAndSet(State.REFLUSH, State.FLUSH);
        default -> throw new IllegalStateException("should not happen");
      }
      next = queue.poll();
    }
  }

  @Override public boolean remove(T message) {
    return queue.remove(message);
  }
}
