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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link MessageQueue}
 */
public class MessageQueueTest {

  @Test public void queue() {
    MessageQueue<Boolean> queue = new SimpleMessageQueue<>(new ConcurrentLinkedDeque<>(), v -> { });

    Assert.assertEquals(0, queue.size());
    Assert.assertEquals(MessageQueue.State.IDLE, queue.state());
    
    queue.put(Boolean.TRUE);
    Assert.assertEquals(1, queue.size());
    
    queue.flush();
    Assert.assertEquals(0, queue.size());

    queue.put(Boolean.TRUE);
    Assert.assertEquals(1, queue.size());
    Assert.assertEquals(MessageQueue.State.IDLE, queue.state());
    
    queue.remove(Boolean.TRUE);
    Assert.assertEquals(0, queue.size());

    queue.put(Boolean.FALSE);
    Assert.assertEquals(1, queue.size());
    
    queue.tryInterrupt();
    Assert.assertEquals(1, queue.size());
    Assert.assertEquals(MessageQueue.State.INTERRUPT, queue.state());

    queue.putAndFlush(Boolean.TRUE);
    Assert.assertEquals(2, queue.size());
    Assert.assertEquals(MessageQueue.State.INTERRUPT, queue.state());
  
    queue.resume();
    Assert.assertEquals(0, queue.size());
    Assert.assertEquals(MessageQueue.State.IDLE, queue.state());
  }

  @Test public void state() {
    Assert.assertEquals(0x00, MessageQueue.State.IDLE.state());
    Assert.assertEquals(0x01, MessageQueue.State.FLUSH.state());
    Assert.assertEquals(0x02, MessageQueue.State.REFLUSH.state());
    Assert.assertEquals(0x04, MessageQueue.State.INTERRUPT.state());
  }
}
