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

package org.nightcode.common.service;

import org.nightcode.common.lang.Event;
import org.nightcode.common.logging.Log;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.nightcode.common.service.Service.State.FAILED;
import static org.nightcode.common.service.Service.State.NEW;
import static org.nightcode.common.service.Service.State.RUNNING;
import static org.nightcode.common.service.Service.State.STARTING;
import static org.nightcode.common.service.Service.State.TERMINATED;

/**
 * Provides default implementations of Service execution methods.
 */
public abstract class AbstractService implements Service {

  static class ServiceEvent implements Event<Service, State> {
    private final Service service;
    private final State   type;

    ServiceEvent(Service service, State type) {
      this.service = service;
      this.type    = type;
    }

    @Override public Service subject() {
      return service;
    }

    @Override public State type() {
      return type;
    }
  }

  final AtomicReference<State> state = new AtomicReference<>(NEW);

  private volatile boolean shutdownWhenStartupFinishes = false;

  private final ReentrantLock lock = new ReentrantLock();

  private final CompletableFuture<Service> startFuture = new CompletableFuture<>();
  private final CompletableFuture<Service> stopFuture = new CompletableFuture<>();

  private final Set<StateListener> listeners = new CopyOnWriteArraySet<>();

  protected AbstractService() {
    addEventListener(event -> {
      if (event.type() == FAILED) {
        Log.warn().log(getClass(), () -> "state changed to " + event.type() + " " + event.subject().failureCause());
      } else {
        Log.info().log(getClass(), "state changed to {}", event.type());
      }
    });
  }

  @Override public void addEventListener(StateListener listener) {
    listeners.add(listener);
  }

  @Override public Throwable failureCause() {
    if (stopFuture.isCompletedExceptionally()) {
      try {
        stopFuture.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        return e.getCause();
      }
    }
    throw new IllegalStateException("service has not failed, state: " + state.get());
  }

  @Override public boolean isRunning() {
    return RUNNING.equals(state.get());
  }

  @Override public void removeEventListener(StateListener listener) {
    listeners.remove(listener);
  }

  @Override public CompletableFuture<Service> startAsync() {
    State s = state.get();
    if (s.equals(NEW)) {
      final ReentrantLock mainLock = this.lock;
      mainLock.lock();
      try {
        if (state.compareAndSet(s, STARTING)) {
          fireStateEvent(State.STARTING);
          doStart();
        }
      } catch (Throwable th) {
        notifyFailed(th);
      } finally {
        mainLock.unlock();
      }
    }
    return startFuture;
  }

  @Override public State state() {
    return state.get();
  }

  @Override public CompletableFuture<Service> stopAsync() {
    State s = state.get();
    if (s.compareTo(State.RUNNING) <= 0) {
      final ReentrantLock mainLock = this.lock;
      mainLock.lock();
      try {
        State previous = state.get();
        switch (previous) {
          case NEW -> notifyStopped();
          case STARTING -> shutdownWhenStartupFinishes = true;
          case RUNNING -> {
            if (state.compareAndSet(previous, State.STOPPING)) {
              fireStateEvent(State.STOPPING);
              doStop();
            }
          }
          default -> throw new AssertionError("should not happen, state:" + previous);
        }
      } catch (Throwable th) {
        notifyFailed(th);
      } finally {
        mainLock.unlock();
      }
    }
    return stopFuture;
  }

  @Override public String toString() {
    return serviceName() + '[' + state() + ']';
  }

  /**
   * This method should be used to initiate service startup.
   * It will cause the service to call {@link #notifyStarted()}.
   * If startup fails, the invocation should cause
   * the service to call {@link #notifyFailed(Throwable)}.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown.
   * It will cause the service to call {@link #notifyStopped()}.
   * If shutdown fails, the invocation should cause
   * the service to call {@link #notifyFailed(Throwable)}.
   */
  protected abstract void doStop();

  protected final void notifyFailed(Throwable cause) {
    Objects.requireNonNull(cause, "cause");
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      State s = state.get();
      state.set(FAILED);
      if (s.compareTo(State.RUNNING) <= 0) {
        startFuture.completeExceptionally(cause);
        stopFuture.completeExceptionally(cause);
        fireStateEvent(State.FAILED);
      } else if (s.compareTo(State.TERMINATED) <= 0) {
        stopFuture.completeExceptionally(cause);
        fireStateEvent(State.FAILED);
      }
    } finally {
      mainLock.unlock();
    }
  }

  protected final void notifyStarted() {
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      State s = state.get();
      if (s != STARTING) {
        IllegalStateException failure = new IllegalStateException("cannot notifyStarted() when the service is " + s);
        notifyFailed(failure);
        throw failure;
      }

      if (state.compareAndSet(s, RUNNING)) {
        if (shutdownWhenStartupFinishes) {
          fireStateEvent(RUNNING);
          stopAsync();
        } else {
          startFuture.complete(this);
          fireStateEvent(RUNNING);
        }
      }
    } finally {
      mainLock.unlock();
    }
  }

  protected final void notifyStopped() {
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      state.set(TERMINATED);
      startFuture.complete(this);
      stopFuture.complete(this);
      fireStateEvent(State.TERMINATED);
    } finally {
      mainLock.unlock();
    }
  }

  public String serviceName() {
    return getClass().getSimpleName();
  }

  private void fireEvent(ServiceEvent event) {
    for (StateListener listener : listeners) {
      listener.onEvent(event);
    }
  }

  private void fireStateEvent(State state) {
    fireEvent(new ServiceEvent(this, state));
  }
}
