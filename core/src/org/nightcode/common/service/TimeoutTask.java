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

import org.nightcode.common.service.TimeoutHandler.TimeoutReason;

/**
 * @param <M>
 */
public final class TimeoutTask<M extends AbstractMessage> implements Runnable {

  private final M message;
  private final TimeoutHandler<M> handler;
  private final TimeoutReason timeoutReason;

  /**
   * @param message
   * @param handler
   * @param timeoutReason
   */
  public TimeoutTask(M message, TimeoutHandler<M> handler, TimeoutReason timeoutReason) {
    this.message = message;
    this.handler = handler;
    this.timeoutReason = timeoutReason;
  }

  @Override public void run() {
    handler.handleTimeout(message, timeoutReason);
  }

  @Override public int hashCode() {
    int result = 17;
    result = result * 31 + (handler != null ? handler.hashCode() : 0);
    result = result * 31 + (message != null ? message.hashCode() : 0);
    return result;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TimeoutTask)) {
      return false;
    }
    TimeoutTask other = (TimeoutTask) obj;
    return (handler == other.handler || (handler != null && handler.equals(other.handler)))
        && (message == other.message || (message != null && message.equals(other.message)));
  }
}
