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

import org.nightcode.common.base.Throwables;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link AbstractMessageService}.
 */
public class AbstractMessageServiceTest {
  
  @Test public void submit() throws Exception {
    final AtomicBoolean value = new AtomicBoolean(false);
    final AtomicInteger counter = new AtomicInteger(0);
    MessageService<Boolean> service = new AbstractMessageService<Boolean>("MessageServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }

      @Override protected void process(Boolean message) throws Exception {
        value.set(message);
        counter.incrementAndGet();
      }
    };
    service.start().get();
    boolean actual = service.submit(Boolean.TRUE);
    assertEquals(true, actual);
    assertEquals(true, value.get());
    assertEquals(1, counter.get());
    service.stop().get();
  }
  
  @Test public void submitFailed() throws Exception {
    final AtomicInteger counter = new AtomicInteger(0);
    MessageService<Boolean> service = new AbstractMessageService<Boolean>("MessageServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }

      @Override protected void process(Boolean message) throws Exception {
        counter.incrementAndGet();
        throw new Exception("This service always throws exception when calling process() method.");
      }
    };
    service.start().get();
    boolean actual = service.submit(Boolean.TRUE);
    assertEquals(false, actual);
    assertEquals(1, counter.get());
    service.stop().get();
  }

  @Test public void submitFailedPropagate() throws Exception {
    final AtomicInteger counter = new AtomicInteger(0);
    MessageService<Boolean> service
        = new AbstractMessageService<Boolean>("MessageServiceTest", true) {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }

      @Override protected void process(Boolean message) throws Exception {
        counter.incrementAndGet();
        throw new Exception("This service always throws exception when calling process() method.");
      }
    };
    service.start().get();
    try {
      service.submit(Boolean.TRUE);
      Assert.fail();
    } catch(Exception ex) {
      Assert.assertEquals("This service always throws exception when calling process() method."
          , Throwables.getRootCause(ex).getMessage());
    }
    assertEquals(1, counter.get());
    service.stop().get();
  }
}
