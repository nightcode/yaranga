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
import java.util.concurrent.Executor;

import static org.nightcode.common.util.ExecutorUtils.namedThreadFactory;

/**
 * A Service that executes logic in separate executor.
 */
public abstract class AbstractExecutorService implements Service {

  private final Service delegate = new AbstractService() {
    @Override protected void doStart() {
      executor().execute(() -> {
        try {
          startUp();
          notifyStarted();
        } catch (Throwable t) {
          notifyFailed(t);
        }
      });
    }

    @Override protected void doStop() {
      executor().execute(() -> {
        try {
          shutDown();
          notifyStopped();
        } catch (Throwable t) {
          notifyFailed(t);
        }
      });
    }

    @Override public String toString() {
      return AbstractExecutorService.this.toString();
    }
  };

  @Override public void addEventListener(StateListener listener) {
    delegate.addEventListener(listener);
  }

  @Override public Throwable failureCause() {
    return delegate.failureCause();
  }

  @Override public void removeEventListener(StateListener listener) {
    delegate.removeEventListener(listener);
  }

  @Override public final boolean isRunning() {
    return delegate.isRunning();
  }

  @Override public final State state() {
    return delegate.state();
  }

  @Override public final CompletableFuture<Service> startAsync() {
    return delegate.startAsync().thenApply(r -> AbstractExecutorService.this);
  }

  @Override public final CompletableFuture<Service> stopAsync() {
    return delegate.stopAsync().thenApply(r -> AbstractExecutorService.this);
  }

  @Override public String toString() {
    return serviceName() + "[" + state() + "]";
  }

  protected abstract void startUp() throws Exception;

  protected abstract void shutDown() throws Exception;

  protected Executor executor() {
    return command -> namedThreadFactory(serviceName()).newThread(command).start();
  }

  public String serviceName() {
    return getClass().getSimpleName();
  }
}
