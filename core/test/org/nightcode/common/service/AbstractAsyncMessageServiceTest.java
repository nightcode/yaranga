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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link AbstractAsyncMessageService}.
 */
public class AbstractAsyncMessageServiceTest {

  @Test public void submit() throws Exception {    
    @SuppressWarnings("unchecked")
    BlockingQueue<Boolean> mockQueue = EasyMock.createMock(BlockingQueue.class);

    EasyMock.expect(mockQueue.take()).andReturn(Boolean.TRUE).once();
    EasyMock.expect(mockQueue.remainingCapacity()).andReturn(1).once();
    EasyMock.expect(mockQueue.offer(Boolean.TRUE, 100, TimeUnit.MILLISECONDS)).andReturn(true).once();

    EasyMock.replay(mockQueue);
    
    MessageService<Boolean> service = new AbstractAsyncMessageService<Boolean>("test", mockQueue) {
      @Override protected void process(Boolean message) throws Exception {
        Thread.sleep(Long.MAX_VALUE);
      }
    };
    service.start().get();
    service.submit(Boolean.TRUE);

    Thread.sleep(100);
    
    EasyMock.verify(mockQueue);
  }
  
  @Test public void skipStrategy() throws Exception {
    MessageService<Boolean> service = new AbstractAsyncMessageService<Boolean>("test",
        new ArrayBlockingQueue<Boolean>(1), true) {
      @Override protected void process(Boolean message) throws Exception {
        Thread.sleep(Long.MAX_VALUE);
      }
    };
    service.start().get();
    assertTrue(service.submit(Boolean.TRUE));
    Thread.sleep(100);
    assertTrue(service.submit(Boolean.TRUE));
    assertFalse(service.submit(Boolean.TRUE));
  }
}
