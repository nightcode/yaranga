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

    void await() throws InterruptedException {
      acquireSharedInterruptibly(1);
    }

    boolean await(long timeout, TimeUnit unit) throws InterruptedException {
      return tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    boolean done(V result, Throwable exception) {
      boolean status = compareAndSetState(RUNNING, PROCESSING);
      if (status) {
        this.result = result;
        this.exception = exception;
        releaseShared(DONE);
      } else if (getState() == PROCESSING) {
        acquireShared(-1);
      }
      return status;
    }

    V get() throws ExecutionException {
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
    sync.await();
    return sync.get();
  }

  @Override public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (!sync.await(timeout, unit)) {
      throw new TimeoutException();
    }
    return sync.get();
  }

  protected boolean failed(Throwable cause) {
    return sync.done(null, cause);
  }

  protected boolean succeeded(V result) {
    return sync.done(result, null);
  }
}
