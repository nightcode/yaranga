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

import java.util.concurrent.CompletableFuture;

/**
 * A Service that executes logic in separate thread.
 */
public abstract class AbstractThreadService extends AbstractService implements Service {

  private volatile boolean operates = true;
  private volatile boolean restart = false;

  private long restartTimeoutMs = 10L; // timeout, in milliseconds

  private final Thread thread;

  protected AbstractThreadService(final String serviceName) {
    super(serviceName);

    thread = new Thread(() -> {
      boolean interrupted = false;
      try {
        onStart();
        started();

        if (isRunning()) {
          Exception lastFailedCause = null;
          try {
            while (operates) {
              try {
                if (lastFailedCause != null) {
                  Exception tmpException = lastFailedCause;
                  lastFailedCause = null;
                  onStart();
                  logger.debug(tmpException, "[%s]: service has been restarted", serviceName());
                }
                service();
              } catch (InterruptedException ex) {
                logger.warn(ex, "[%s]: service has been interrupted", serviceName());
                interrupted = true;
                if (restart) {
                  restart = false;
                  lastFailedCause = ex;
                  try {
                    onStop();
                  } catch (Exception ex2) {
                    logger.trace(ex2, "[%s]: exception occurred", serviceName());
                  }
                } else {
                  break;
                }
              } catch (Exception ex) {
                logger.warn(ex, "[%s]: service's exception", serviceName());
                lastFailedCause = ex;
                try {
                  onStop();
                } catch (Exception ex2) {
                  logger.trace(ex2, "[%s]: exception occurred", serviceName());
                }
                try {
                  Thread.sleep(restartTimeoutMs);
                } catch (InterruptedException interrupt) {
                  logger.trace(interrupt, "[%s]: exception occurred", serviceName());
                }
              }
            }
          } catch (Throwable th) {
            th.printStackTrace();
            logger.fatal(th, "[%s]: Service will be stopped. Unexpected error.", serviceName());
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
    }, serviceName());
  }

  public final void interrupt() {
    thread.interrupt();
  }

  @Override public final CompletableFuture<State> start() {
    startUp();
    return super.start();
  }

  @Override public final CompletableFuture<State> stop() {
    return super.stop();
  }

  @Override protected final void doStart() {
    thread.start();
  }

  @Override protected final void doStop() {
    operates = false;
    thread.interrupt();
  }

  protected boolean isOperates() {
    return operates;
  }

  protected void onStart() throws Exception  {
    // do nothing
  }

  protected void onStop() throws Exception  {
    // do nothing
  }

  protected abstract void service() throws Exception;

  protected final void setRestartTimeout(long restartTimeoutMs) {
    this.restartTimeoutMs = restartTimeoutMs;
  }

  protected void startUp() {
    // do nothing
  }

  protected final void restart() {
    restart = true;
    thread.interrupt();
  }
}
