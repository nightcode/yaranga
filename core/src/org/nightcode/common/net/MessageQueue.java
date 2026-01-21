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

package org.nightcode.common.net;

/**
 * MessageQueue interface.
 *
 * @param <M> message type
 */
public interface MessageQueue<M> {

  enum State {
    IDLE(0x00),
    FLUSH(0x01),
    REFLUSH(0x02),
    INTERRUPT(0x04);

    private final int state;

    State(int state) {
      this.state = state;
    }

    public int state() {
      return state;
    }
  }

  void flush();

  int size();

  State state();

  void resume();

  void tryInterrupt();

  void put(M message);

  void putAndFlush(M message);

  boolean remove(M message);
}
