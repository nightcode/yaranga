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

/**
 * A Service that executes logic in separate thread.
 */
public abstract class AbstractThreadService implements Service {

  protected static final ServiceLogger LOGGER
      = ServiceLogger.getLogger(AbstractThreadService.class.getName());

  protected volatile boolean exit = false;
  private volatile boolean restart = false;

  private long restartTimeout = 10L; // timeout, in milliseconds

  private final Service inner;
  private volatile Thread thread;

  protected AbstractThreadService(String serviceName) {
    inner = new AbstractService(serviceName) {
      @Override protected final void doStart() {
        thread = new Thread(new Runnable() {
          @Override public void run() {
            try {
              onStart();
              started();

              if (state() == State.RUNNING) {
                Exception lastFailedCause = null;
                try {
                  while (!exit) {
                    try {
                      if (lastFailedCause != null) {
                        Exception tmpException = lastFailedCause;
                        lastFailedCause = null;
                        onStart();
                        AbstractThreadService.LOGGER.log(Level.FINE
                            , "[%s]: service has been restarted", tmpException, serviceName());
                      }
                      service();
                    } catch (InterruptedException interrupted) {
                      AbstractThreadService.LOGGER.log(Level.WARNING
                          , "[%s]: service has been interrupted", interrupted, serviceName());
                      if (restart) {
                        restart = false;
                        lastFailedCause = interrupted;
                        try {
                          onStop();
                        } catch (Exception ignore) {
                          AbstractThreadService.LOGGER.log(Level.FINEST, "[%s]: exception occurred"
                              , ignore, serviceName());
                        }
                      }
                    } catch (Exception ex) {
                      AbstractThreadService.LOGGER.log(Level.WARNING, "[%s]: service's exception"
                          , ex, serviceName());
                      lastFailedCause = ex;
                      try {
                        onStop();
                      } catch (Exception ignore) {
                        AbstractThreadService.LOGGER.log(Level.FINEST, "[%s]: exception occurred"
                            , ignore, serviceName());
                      }
                      try {
                        Thread.sleep(restartTimeout);
                      } catch (InterruptedException ignore) {
                        AbstractThreadService.LOGGER.log(Level.FINEST, "[%s]: exception occurred"
                            , ignore, serviceName());
                      }
                    }
                  }
                } catch (Throwable th) {
                  th.printStackTrace();
                  AbstractThreadService.LOGGER.log(Level.SEVERE
                      , "[%s]: Service will be stopped. Unexpected error.", th, serviceName());
                }
              }

              if (state() == State.RUNNING || state() == State.STOPPING) {
                onStop();
              }
              stopped();
            } catch (Throwable th) {
              serviceFailed(th);
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

  @Override public final State state() {
    return inner.state();
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

  protected final void restart() {
    restart = true;
    thread.interrupt();
  }

  protected abstract void service() throws Exception;

  protected final void setRestartTimeout(long restartTimeout) {
    this.restartTimeout = restartTimeout;
  }

  protected void startUp() {
    // do nothing
  }
}
