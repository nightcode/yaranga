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

import java.util.concurrent.ConcurrentLinkedDeque;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MessageQueue}
 */
public class MessageQueueTest {

  @Test public void queue() {
    MessageQueue<Boolean> queue = new SimpleMessageQueue<>(new ConcurrentLinkedDeque<>(), v -> { });

    assertEquals(0, queue.size());
    assertEquals(MessageQueue.State.IDLE, queue.state());

    queue.put(Boolean.TRUE);
    assertEquals(1, queue.size());

    queue.flush();
    assertEquals(0, queue.size());

    queue.put(Boolean.TRUE);
    assertEquals(1, queue.size());
    assertEquals(MessageQueue.State.IDLE, queue.state());

    queue.remove(Boolean.TRUE);
    assertEquals(0, queue.size());

    queue.put(Boolean.FALSE);
    assertEquals(1, queue.size());

    queue.tryInterrupt();
    assertEquals(1, queue.size());
    assertEquals(MessageQueue.State.INTERRUPT, queue.state());

    queue.putAndFlush(Boolean.TRUE);
    assertEquals(2, queue.size());
    assertEquals(MessageQueue.State.INTERRUPT, queue.state());

    queue.resume();
    assertEquals(0, queue.size());
    assertEquals(MessageQueue.State.IDLE, queue.state());
  }

  @Test public void state() {
    assertEquals(0x00, MessageQueue.State.IDLE.state());
    assertEquals(0x01, MessageQueue.State.FLUSH.state());
    assertEquals(0x02, MessageQueue.State.REFLUSH.state());
    assertEquals(0x04, MessageQueue.State.INTERRUPT.state());
  }
}
