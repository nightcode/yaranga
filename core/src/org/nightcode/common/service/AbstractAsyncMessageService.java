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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
          LOGGER.log(Level.INFO, () -> String.format("[%s]: message <%s> has been skipped (queue remaining capacity %s)"
              , serviceName(), message, queue.remainingCapacity()));
          return false;
        }
        return true;
      }
    } else {
      if (queue.remainingCapacity() == 0) {
        LOGGER.log(Level.INFO, () -> String
            .format("[%s]: queue capacity has been reached <%s> (waiting for space to become available)"
                , serviceName(), queue.size()));
      }
      for (;;) {
        try {
          if (queue.offer(message, 100, TimeUnit.MILLISECONDS)) {
            int recheck = state();
            if (!AbstractService.isRunning(recheck) && queue.remove(message)) {
              LOGGER.log(Level.INFO
                  , () -> String.format("[%s]: message <%s> has been rejected", serviceName(), message));
              return false;
            }
            return true;
          }
        } catch (InterruptedException ex) {
          LOGGER.log(Level.WARNING, ex, () -> String.format("[%s]: exception:", serviceName()));
          Thread.currentThread().interrupt();
        }
      }
    }
    return false;
  }

  protected abstract void process(M message) throws Exception;

  @Override protected void service() throws Exception {
    while (isOperates()) {
      process(queue.take());
    }
  }
}
