/*
 * Copyright (C) 2014 The NightCode Open Source Project
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

package org.nightcode.common.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * Base class for messages.
 */
public abstract class AbstractMessage {

  /**
   * Message states.
   */
  public enum State {
    NEW, RUNNING, TERMINATED, FAILED
  }

  private transient volatile ScheduledFuture<?> timeoutTask;

  private volatile Throwable exception;

  private final Consumer<AbstractMessage> callbackHandler;

  private volatile State state = State.NEW;
  private final ReentrantLock lock = new ReentrantLock();

  @SuppressWarnings("unchecked")
  protected AbstractMessage(@Nullable Consumer<? extends AbstractMessage> callbackHandler) {
    this.callbackHandler = (Consumer<AbstractMessage>) callbackHandler;
  }

  public final boolean callback() throws ExecutionException {
    try {
      lock.lock();
      if (state == State.RUNNING) {
        cancelTimeoutTask();
        state = State.TERMINATED;
        if (callbackHandler != null) {
          try {
            callbackHandler.accept(this);
          } catch (Exception ex) {
            throw new ExecutionException(ex);
          }
        }
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public final boolean callback(Throwable cause) throws ExecutionException {
    try {
      lock.lock();
      if (state == State.NEW || state == State.RUNNING) {
        cancelTimeoutTask();
        state = State.FAILED;
        exception = cause;
        if (callbackHandler != null) {
          try {
            callbackHandler.accept(this);
          } catch (Exception ex) {
            throw new ExecutionException(ex);
          }
        }
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public final Throwable getException() {
    return exception;
  }

  public final ScheduledFuture<?> getTimeoutTask() {
    return timeoutTask;
  }

  public final boolean isNew() {
    return State.NEW.equals(state);
  }

  public final boolean resume() {
    try {
      lock.lock();
      if (state == State.NEW) {
        state = State.RUNNING;
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public final void setTimeoutTask(ScheduledFuture<?> timeoutTask) {
    try {
      lock.lock();
      cancelTimeoutTask();
      this.timeoutTask = timeoutTask;
    } finally {
      lock.unlock();
    }
  }

  private void cancelTimeoutTask() {
    try {
      lock.lock();
      if (timeoutTask != null) {
        timeoutTask.cancel(false);
        timeoutTask = null;
      }
    } finally {
      lock.unlock();
    }
  }
}
