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

import org.nightcode.common.util.logging.LogManager;
import org.nightcode.common.util.logging.Logger;
import org.nightcode.common.util.monitoring.MonitoringManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides default implementations of Service execution methods.
 */
public abstract class AbstractService implements Service {

  private static final int NEW        = 0x00000000;
  private static final int STARTING   = 0x00000001;
  private static final int RUNNING    = 0x00000002;
  private static final int SHUTDOWN   = 0x00000004;
  private static final int STOPPING   = 0x00000008;
  private static final int TERMINATED = 0x00000010;
  private static final int FAILED     = 0x00000020;

  static boolean isRunning(int s) {
    return s == RUNNING;
  }

  protected final Logger logger;
  private final String serviceName;

  final AtomicInteger state = new AtomicInteger(NEW);

  private final ReentrantLock lock = new ReentrantLock();

  private final CompletableFuture<State> startFuture = new CompletableFuture<>();
  private final CompletableFuture<State> stopFuture = new CompletableFuture<>();

  private volatile boolean stopAfterStart = false;

  protected AbstractService(String serviceName) {
    this.serviceName = serviceName;
    this.logger = LogManager.getLogger(this);

    MonitoringManager.registerGauge(serviceName + ".status", state::get);
  }

  @Override public String serviceName() {
    return serviceName;
  }

  @Override public CompletableFuture<State> start() {
    int s = state.get();
    if (s < RUNNING) {
      final ReentrantLock mainLock = this.lock;
      mainLock.lock();
      try {
        if (state.compareAndSet(s, STARTING)) {
          logger.debug("[%s]: starting service..", serviceName);
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

  @Override public CompletableFuture<State> stop() {
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
          logger.debug("[%s]: stopping service..", serviceName);
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
    String st = stateAsString();
    return serviceName + '[' + st + ']';
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

  protected final boolean isClosing() {
    return state.get() > RUNNING;
  }

  protected final boolean isRunning() {
    return state.get() == RUNNING;
  }

  protected final boolean isStopping() {
    return state.get() == STOPPING;
  }

  protected final void serviceFailed(Throwable cause) {
    Objects.requireNonNull(cause, "cause");
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      int s = state.get();
      state.set(FAILED);
      if (s < RUNNING) {
        logger.warn(cause, "[%s]: exception occurred while starting service:", serviceName);
        startFuture.completeExceptionally(cause);
        stopFuture.completeExceptionally(new Exception("service failed to start", cause));
      } else if (s < TERMINATED) {
        logger.warn(cause, "[%s]: exception occurred while stopping service:", serviceName);
        stopFuture.completeExceptionally(cause);
      }
    } finally {
      mainLock.unlock();
    }
  }

  protected final void started() {
    int s = state.get();
    if (s != STARTING) {
      throw new IllegalStateException("cannot start service when it is " + s);
    }

    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      if (state.compareAndSet(s, RUNNING)) {
        logger.info("[%s]: service has been started", serviceName);
        if (stopAfterStart) {
          stop();
        } else {
          startFuture.complete(State.RUNNING);
        }
      }
    } finally {
      mainLock.unlock();
    }
  }

  protected final int state() {
    return state.get();
  }

  protected final String stateAsString() {
    int s = state.get();
    return s < STARTING ? "NEW" : s < RUNNING
        ? "STARTING" : s < SHUTDOWN
        ? "RUNNING" : s < STOPPING
        ? "SHUTDOWN" : s < TERMINATED
        ? "STOPPING" : s < FAILED
        ? "TERMINATED" : "FAILED";
  }

  protected final void stopped() {
    final ReentrantLock mainLock = this.lock;
    mainLock.lock();
    try {
      state.set(TERMINATED);
      logger.info("[%s]: service has been stopped", serviceName);
      startFuture.complete(State.TERMINATED);
      stopFuture.complete(State.TERMINATED);
    } finally {
      mainLock.unlock();
    }
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
