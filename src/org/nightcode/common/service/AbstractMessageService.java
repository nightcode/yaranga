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

/**
 * Abstract message service.
 * @param <M> The message type accepted by this MessageService's <tt>submit</tt> method
 */
public abstract class AbstractMessageService<M> extends AbstractService
    implements MessageService<M> {

  protected AbstractMessageService(String serviceName) {
    super(serviceName);
  }

  @Override public boolean submit(M message) {
    lock.lock();
    try {
      process(message);
      return true;
    } catch (Exception ex) {
      LOGGER.warning("[%s]: Exception occurred while submitting message <%s>."
          , ex, serviceName(), message);
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Processes message.
   *
   * @param message message for processing
   * @throws Exception exception
   */
  protected abstract void process(M message) throws Exception;
}
