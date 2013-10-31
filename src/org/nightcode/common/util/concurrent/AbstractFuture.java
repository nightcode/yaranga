/*
 * Copyright (C) 2008 The NightCode Open Source Project
 *
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

package org.nightcode.common.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import javax.annotation.Nonnull;

/**
 * An AbstractFuture object.
 *
 * @param <V> The result type returned by this Future's <tt>get</tt> method
 */
public abstract class AbstractFuture<V> implements Future<V> {

  private static final class Sync<V> extends AbstractQueuedSynchronizer {

    static final int RUNNING = 0;
    static final int PROCESSING = 1;
    static final int DONE = 2;

    private V result;
    private Throwable exception;

    /**
     * Creates a new <tt>Sync</tt> instance.
     * Initial synchronization state equals to zero (RUNNING).
     */
    Sync() {
      // do nothing
    }

    public int tryAcquireShared(int ignore) {
      return isDone() ? 1 : -1;
    }

    public boolean tryReleaseShared(int state) {
      setState(state);
      return true;
    }

    boolean done(V result, Throwable exception) {
      boolean status = compareAndSetState(RUNNING, PROCESSING);
      if (status) {
        this.result = result;
        this.exception = exception;
        releaseShared(DONE);
      } else if (getState() == PROCESSING) {
        acquireShared(0); // 0 - magic, can be used any value
      }
      return status;
    }

    V get() throws ExecutionException, InterruptedException {
      acquireSharedInterruptibly(0); // 0 - magic, can be used any value
      int state = getState();
      if (state != DONE) {
        throw new IllegalStateException("Invalid state <" + state + ">");
      }
      if (exception != null) {
        throw new ExecutionException(exception);
      }
      return result;
    }

    V get(long timeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
      if (!tryAcquireSharedNanos(0, unit.toNanos(timeout))) { // 0 - magic, can be used any value
        throw new TimeoutException();
      }
      int state = getState();
      if (state != DONE) {
        throw new IllegalStateException("Invalid state <" + state + ">");
      }
      if (exception != null) {
        throw new ExecutionException(exception);
      }
      return result;
    }

    boolean isDone() {
      return (getState() & DONE) != 0;
    }
  }

  private final Sync<V> sync = new Sync<V>();

  @Override public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override public boolean isCancelled() {
    return false;
  }

  @Override public boolean isDone() {
    return sync.isDone();
  }

  @Override public V get() throws InterruptedException, ExecutionException {
    return sync.get();
  }

  @Override public V get(long timeout, @Nonnull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return sync.get(timeout, unit);
  }

  protected boolean failed(Throwable cause) {
    return sync.done(null, cause);
  }

  protected boolean succeeded(V result) {
    return sync.done(result, null);
  }
}
