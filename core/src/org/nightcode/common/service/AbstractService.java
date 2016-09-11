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

import org.nightcode.common.util.concurrent.AbstractFuture;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  protected static final Logger LOGGER = Logger.getLogger(AbstractService.class.getName());

  private static final int NEW        = 0x00000000;
  private static final int STARTING   = 0x00000001;
  private static final int RUNNING    = 0x00000002;
  private static final int SHUTDOWN   = 0x00000004;
  private static final int STOPPING   = 0x00000008;
  private static final int TERMINATED = 0x00000010;
  private static final int FAILED     = 0x00000020;
  
  static boolean isRunning(int s) {
    return s < SHUTDOWN;
  }

  final ReentrantLock lock = new ReentrantLock();

  private final ServiceFuture startFuture = ServiceFuture.newInstance();
  private final ServiceFuture stopFuture = ServiceFuture.newInstance();

  private final String serviceName;

  final AtomicInteger state = new AtomicInteger(NEW);

  private volatile boolean stopAfterStart = false;

  protected AbstractService(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override public String serviceName() {
    return serviceName;
  }

  @Override public final Future<State> start() {
    int s = state.get();
    if (s < RUNNING) {
      final ReentrantLock mainLock = this.lock;
      mainLock.lock();
      try {
        if (state.compareAndSet(s, STARTING)) {
          LOGGER.log(Level.INFO, () -> String.format("[%s]: starting service..", serviceName));
          doStart();
        }
      } catch (Throwable th) {
        serviceFailed(th);
      } finally {
        mainLock.unlock();
      }
    }
    return startFuture;
  }

  @Override public final Future<State> stop() {
    int s = state.get();
    if (s < STOPPING) {
      final ReentrantLock mainLock = this.lock;
      mainLock.lock();
      try {
        s = state.get();
        if (s < STARTING) {
          stopped();
        } else if (s < RUNNING) {
          stopAfterStart = true;
        } else if (s < STOPPING && state.compareAndSet(s, STOPPING)) {
          LOGGER.log(Level.INFO, () -> String.format("[%s]: stopping service..", serviceName));
          doStop();
        }
      } catch (Throwable th) {
        serviceFailed(th);
      } finally {
        mainLock.unlock();
      }
    }
    return stopFuture;
  }

  @Override public String toString() {
    int s = state.get();
    String st = s < STARTING ? "NEW" : s < RUNNING
        ? "STARTING" : s < SHUTDOWN
        ? "RUNNING" : s < STOPPING
        ? "SHUTDOWN" : s < TERMINATED
        ? "STOPPING" : s < FAILED
        ? "TERMINATED" : "FAILED";
    return serviceName() + '[' + st + ']';
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
    Objects.requireNonNull(cause, "cause");
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      int s = state.get();
      if (s < RUNNING) {
        LOGGER.log(Level.WARNING, cause,
            () -> String.format("[%s]: exception occurred while starting service:", serviceName));
        startFuture.failed(cause);
        stopFuture.failed(new Exception("service failed to start", cause));
      } else if (s < TERMINATED) {
        LOGGER.log(Level.WARNING, cause,
            () -> String.format("[%s]: exception occurred while stopping service:", serviceName));
        stopFuture.failed(cause);
      }
      state.set(FAILED);
    } finally {
      mainLock.unlock();
    }
  }

  protected void started() {
    int s = state.get();
    if (s != STARTING) {
      throw new IllegalStateException("cannot start service when it is " + s);
    }
    
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      if (state.compareAndSet(s, RUNNING)) {
        LOGGER.log(Level.INFO, () -> String.format("[%s]: service has been started", serviceName));
        if (stopAfterStart) {
          stop();
        } else {
          startFuture.succeeded(State.RUNNING);
        }
      }
    } finally {
      mainLock.unlock();
    }
  }

  protected void stopped() {
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      state.set(TERMINATED);
      LOGGER.log(Level.INFO, () -> String.format("[%s]: service has been stopped", serviceName));
      startFuture.succeeded(State.TERMINATED);
      stopFuture.succeeded(State.TERMINATED);
    } finally {
      mainLock.unlock();
    }
  }

  boolean isRunning() {
    return state.get() < SHUTDOWN;
  }

  boolean isStopping() {
    return state.get() == STOPPING;
  }

  void shutdown() {
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      transitState(SHUTDOWN);
    } finally {
      mainLock.unlock();
    }
  }

  private void transitState(int update) {
    for (;;) {
      int s = state.get();
      if (s >= update || state.compareAndSet(s, update)) {
        break;
      }
    }
  }
}
