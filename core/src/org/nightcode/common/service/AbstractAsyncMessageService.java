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

import org.nightcode.common.util.logging.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Abstract async message service.
 *
 * @param <M> The message type accepted by this MessageService's <tt>submit</tt> method
 */
public abstract class AbstractAsyncMessageService<M> extends AbstractThreadService
    implements MessageService<M> {

  private static final boolean DEFAULT_SKIP_MESSAGE_STRATEGY = false;

  protected final BlockingQueue<M> queue;
  private final boolean skipMessageStrategy;

  public AbstractAsyncMessageService(String serviceName) {
    this(serviceName, new LinkedBlockingQueue<>(), DEFAULT_SKIP_MESSAGE_STRATEGY);
  }

  public AbstractAsyncMessageService(String serviceName, boolean skipMessageStrategy) {
    this(serviceName, new LinkedBlockingQueue<>(), skipMessageStrategy);
  }

  public AbstractAsyncMessageService(String serviceName, BlockingQueue<M> queue) {
    this(serviceName, queue, DEFAULT_SKIP_MESSAGE_STRATEGY);
  }

  public AbstractAsyncMessageService(String serviceName, BlockingQueue<M> queue,
      boolean skipMessageStrategy) {
    super(serviceName);
    this.queue = queue;
    this.skipMessageStrategy = skipMessageStrategy;
  }

  @Override public int awaitProcessingCount() {
    return queue.size();
  }

  @Override public void shutdown() {
    super.shutdown();
  }

  public boolean submit(M message) {
    int s = state();
    if (!AbstractService.isRunning(s)) {
      return false;
    }

    if (skipMessageStrategy) {
      if (queue.offer(message)) {
        int recheck = state();
        if (!AbstractService.isRunning(recheck) && queue.remove(message)) {
          Log.info().log(getClass(), "[{}]: message <{}> has been skipped (queue remaining capacity {})"
              , serviceName(), message, queue.remainingCapacity());
          return false;
        }
        return true;
      }
    } else {
      for (;;) {
        try {
          if (queue.offer(message, 100, TimeUnit.MILLISECONDS)) {
            int recheck = state();
            if (!AbstractService.isRunning(recheck) && queue.remove(message)) {
              Log.info().log(getClass(), "[{}]: message <{}> has been rejected", serviceName(), message);
              return false;
            }
            return true;
          }
        } catch (InterruptedException ex) {
          Log.warn().log(getClass(), ex, "[{}]: exception:", serviceName());
          Thread.currentThread().interrupt();
          if (!isRunning()) {
            return false;
          }
        }
      }
    }
    return false;
  }

  protected abstract void process(M message) throws Exception;

  @Override protected void service() throws Exception {
    while (isOperates()) {
      M msg = queue.poll(100, TimeUnit.MILLISECONDS);
      while (msg != null) {
        process(msg);
        msg = queue.poll();
      }
    }
  }
}
