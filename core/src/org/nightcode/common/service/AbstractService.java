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

package org.nightcode.common.service;

import org.nightcode.common.base.Objects;
import org.nightcode.common.util.concurrent.AbstractFuture;

import java.util.EnumSet;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Provides default implementations of Service execution methods.
 */
public abstract class AbstractService implements Service {

  private static final class ServiceFuture extends AbstractFuture<State> {

    public static ServiceFuture newInstance() {
      return new ServiceFuture();
    }

    private ServiceFuture() {
      // do nothing
    }

    @Override public boolean failed(Throwable cause) {
      return super.failed(cause);
    }

    @Override public boolean succeeded(State result) {
      return super.succeeded(result);
    }
  }

  protected static final ServiceLogger LOGGER
      = ServiceLogger.getLogger(AbstractService.class.getName());

  private static final EnumSet<State> STOPPING_STATES = EnumSet.of(State.RUNNING, State.STOPPING);

  final ReentrantLock lock = new ReentrantLock();

  private final ServiceFuture startFuture = ServiceFuture.newInstance();
  private final ServiceFuture stopFuture = ServiceFuture.newInstance();

  private final String serviceName;

  private State state = State.NEW;

  private volatile boolean stopAfterStart = false;

  protected AbstractService(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override public String serviceName() {
    return serviceName;
  }

  @Override public final Future<State> start() {
    lock.lock();
    try {
      LOGGER.log(Level.INFO, "[%s]: starting service..", serviceName);
      if (state == State.NEW) {
        state = State.STARTING;
        doStart();
      }
    } catch (Throwable th) {
      serviceFailed(th);
    } finally {
      lock.unlock();
    }
    return startFuture;
  }

  @Override public final Future<State> stop() {
    lock.lock();
    try {
      if (state == State.NEW) {
        stopped();
      } else if (state == State.STARTING) {
        stopAfterStart = true;
      } else if (state == State.RUNNING) {
        LOGGER.log(Level.INFO, "[%s]: stopping service..", serviceName);
        state = State.STOPPING;
        doStop();
      }
    } catch (Throwable th) {
      serviceFailed(th);
    } finally {
      lock.unlock();
    }
    return stopFuture;
  }

  @Override public final State state() {
    lock.lock();
    try {
      return state;
    } finally {
      lock.unlock();
    }
  }

  @Override public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(serviceName());
    sb.append('[').append(state()).append(']');
    return sb.toString();
  }

  /**
   * This method should be used to initiate service startup.
   * It will cause the service to call {@link #started()}.
   * If startup fails, the invocation should cause
   * the service to call {@link #serviceFailed(Throwable)}.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown.
   * It will cause the service to call {@link #stopped()}.
   * If shutdown fails, the invocation should cause
   * the service to call {@link #serviceFailed(Throwable)}.
   */
  protected abstract void doStop();

  protected void serviceFailed(Throwable cause) {
    Objects.nonNull(cause, "cause");
    lock.lock();
    try {
      if (state == State.STARTING) {
        LOGGER.log(Level.WARNING, "[%s]: exception occurred while starting service:", cause
            , serviceName);
        startFuture.failed(cause);
        stopFuture.failed(new Exception("service failed to start", cause));
      } else if (STOPPING_STATES.contains(state)) {
        LOGGER.log(Level.WARNING, "[%s]: exception occurred while stopping service:", cause
            , serviceName);
        stopFuture.failed(cause);
      }
      state = State.FAILED;
    } finally {
      lock.unlock();
    }
  }

  protected void started() {
    lock.lock();
    try {
      Objects.validState(state == State.STARTING, "cannot started service when it is %s", state);
      state = State.RUNNING;
      LOGGER.log(Level.INFO, "[%s]: service has been started", serviceName);
      if (stopAfterStart) {
        stop();
      } else {
        startFuture.succeeded(State.RUNNING);
      }
    } finally {
      lock.unlock();
    }
  }

  protected void stopped() {
    lock.lock();
    try {
      state = State.TERMINATED;
      LOGGER.log(Level.INFO, "[%s]: service has been stopped", serviceName);
      startFuture.succeeded(State.TERMINATED);
      stopFuture.succeeded(State.TERMINATED);
    } finally {
      lock.unlock();
    }
  }

  void setState(State state) {
    lock.lock();
    try {
      this.state = state;
    } finally {
      lock.unlock();
    }
  }
}
