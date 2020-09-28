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

import org.nightcode.common.base.Throwables;
import org.nightcode.common.util.monitoring.Counter;
import org.nightcode.common.util.monitoring.MonitoringManager;

/**
 * Abstract message service.
 * @param <M> The message type accepted by this MessageService's <tt>submit</tt> method
 */
public abstract class AbstractMessageService<M> extends AbstractService implements MessageService<M> {

  private final boolean propagateException;

  private final Counter submitted;

  protected AbstractMessageService(String serviceName) {
    this(serviceName, false);
  }

  protected AbstractMessageService(String serviceName, boolean propagateException) {
    super(serviceName);
    this.propagateException = propagateException;

    submitted = MonitoringManager.registerCounter(serviceName + ".messages.Submitted");
  }

  @Override public int awaitProcessingCount() {
    return 0;
  }

  @Override public void shutdown() {
    super.shutdown();
  }

  @Override public boolean submit(M message) {
    submitted.inc();
    if (isRunning()) {
      try {
        process(message);
        return true;
      } catch (Exception ex) {
        if (propagateException) {
          throw Throwables.propagate(ex);
        }
        logger.warn(ex, "[%s]: exception occurred while submitting message <%s>", serviceName(), message);
      }
    }
    return false;
  }

  /**
   * Processes message.
   *
   * @param message message for processing
   * @throws Exception exception
   */
  protected abstract void process(M message) throws Exception;
}
