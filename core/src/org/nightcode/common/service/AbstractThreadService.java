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

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Service that executes logic in separate thread.
 */
public abstract class AbstractThreadService implements Service {

  protected static final Logger LOGGER = Logger.getLogger(AbstractThreadService.class.getName());

  volatile boolean exit = false;

  private long restartTimeout = 10L; // timeout, in milliseconds

  private final AbstractService inner;
  private volatile Thread thread;

  protected AbstractThreadService(final String serviceName) {
    inner = new AbstractService(serviceName) {
      @Override protected final void doStart() {
        thread = new Thread(() -> {
          boolean interrupted = false;
          try {
            onStart();
            started();

            if (isRunning()) {
              Exception lastFailedCause = null;
              try {
                while (!exit) {
                  try {
                    if (lastFailedCause != null) {
                      Exception tmpException = lastFailedCause;
                      lastFailedCause = null;
                      onStart();
                      AbstractThreadService.LOGGER.log(Level.FINE, tmpException
                          , () -> String.format("[%s]: service has been restarted", serviceName()));
                    }
                    service();
                  } catch (InterruptedException ex) {
                    AbstractThreadService.LOGGER.log(Level.WARNING, ex 
                        , () -> String.format("[%s]: service has been interrupted", serviceName()));
                    interrupted = true;
                    break;
                  } catch (Exception ex) {
                    AbstractThreadService.LOGGER.log(Level.WARNING, ex
                        , () -> String.format("[%s]: service's exception", serviceName()));
                    lastFailedCause = ex;
                    try {
                      onStop();
                    } catch (Exception ignore) {
                      AbstractThreadService.LOGGER.log(Level.FINEST, ignore
                          , () -> String.format("[%s]: exception occurred", serviceName()));
                    }
                    try {
                      Thread.sleep(restartTimeout);
                    } catch (InterruptedException ignore) {
                      AbstractThreadService.LOGGER.log(Level.FINEST, ignore
                          , () -> String.format("[%s]: exception occurred", serviceName()));
                    }
                  }
                }
              } catch (Throwable th) {
                th.printStackTrace();
                AbstractThreadService.LOGGER.log(Level.SEVERE, th, () -> String
                    .format("[%s]: Service will be stopped. Unexpected error.", serviceName()));
              }
            }

            if (isStopping() || interrupted) {
              onStop();
            }
            stopped();
          } catch (Throwable th) {
            serviceFailed(th);
          } finally {
            if (interrupted) {
              Thread.currentThread().interrupt();
            }
          }
        }, super.serviceName());
        thread.start();
      }

      @Override protected final void doStop() {
        exit = true;
        thread.interrupt();
      }
    };
  }

  public final void interrupt() {
    thread.interrupt();
  }

  @Override public String serviceName() {
    return inner.serviceName();
  }

  @Override public final Future<State> start() {
    startUp();
    return inner.start();
  }

  @Override public final Future<State> stop() {
    return inner.stop();
  }

  @Override public String toString() {
    return inner.toString();
  }

  protected void onStart() throws Exception  {
    // do nothing
  }

  protected void onStop() throws Exception  {
    // do nothing
  }

  protected abstract void service() throws Exception;

  protected void serviceFailed(Throwable cause) {
    inner.serviceFailed(cause);
  }

  protected final void setRestartTimeout(long restartTimeout) {
    this.restartTimeout = restartTimeout;
  }

  protected void startUp() {
    // do nothing
  }

  void shutdown() {
    inner.shutdown();
  }

  final int state() {
    return inner.state.get();
  }
}
