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

import org.nightcode.common.service.Service.State;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link AbstractThreadService}.
 */
public class AbstractThreadServiceTest {
    
  @Test public void getServiceName() {
    Service service = new AbstractThreadService("ThreadServiceTest") {
      @Override protected void service() throws Exception {
        // do nothing
      }
    };
    assertEquals("ThreadServiceTest", service.serviceName());
  }
    
  @Test public void toStringPrint() throws ExecutionException, InterruptedException {
    Service service = new AbstractThreadService("ThreadServiceTest") {
      @Override protected void service() throws Exception {
        Thread.sleep(Integer.MAX_VALUE);
      }
    };
    assertEquals("ThreadServiceTest[" + State.NEW + "]", service.toString());
    service.start().get();
    assertEquals("ThreadServiceTest[" + State.RUNNING + "]", service.toString());
    service.stop().get();
    assertEquals("ThreadServiceTest[" + State.TERMINATED + "]", service.toString());
  }
  
  @Test public void startCalled() throws ExecutionException, InterruptedException {
    Service service = new AbstractThreadService("test") {
      @Override protected void service() throws Exception {
        Thread.sleep(Integer.MAX_VALUE);
      }
    };
    
    assertEquals(State.NEW, service.state());
    service.start().get();
    assertEquals(State.RUNNING, service.state());
  }
  
  @Test public void startCalledException() {
    Service service = new AbstractThreadService("test") {
      @Override protected void onStart() throws Exception {
        throw new RuntimeException("This service always throws exception " 
            + "when calling onStart() method.");
      }

      @Override protected void service() throws Exception {
        throw new AssertionError();
      }
    };
    
    assertEquals(State.NEW, service.state());
    try {
      service.start().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling onStart() method."));
      assertEquals(State.FAILED, service.state());
      return;
    }
    fail();
  }

  @Test public void startCalledExceptionCheckStopFuture() {
    Service service = new AbstractThreadService("test") {
      @Override protected void onStart() throws Exception {
        throw new RuntimeException("This service always throws exception " 
            + "when calling onStart() method.");
      }

      @Override protected void service() throws Exception {
        throw new AssertionError();
      }
    };

    assertEquals(State.NEW, service.state());
    try {
      service.start().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling onStart() method."));
      assertEquals(State.FAILED, service.state());

      try {
        service.stop().get();
      } catch (Throwable t) {
        assertTrue(t.getMessage().contains("Service failed to start."));
        assertEquals(State.FAILED, service.state());
        return;
      }
    }
    fail();
  }
  
  @Test public void stopCalled() throws ExecutionException, InterruptedException {
    Service service = new AbstractThreadService("test") {
      @Override protected void service() throws Exception {
        exit = true;
      }
    };
    assertEquals(State.NEW, service.state());
    service.start().get();
    service.stop().get();
    assertEquals(State.TERMINATED, service.state());
  }
  
  @Test public void stopCalledExceptionIllegalState() 
      throws ExecutionException, InterruptedException {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch stopLatch = new CountDownLatch(1);
    
    Service service = new AbstractThreadService("test") {
      @Override protected void onStart() throws Exception {
        startLatch.countDown();
        try {
          stopLatch.await();
        } catch (InterruptedException ignore) {
          // do nothing
        }
      }

      @Override protected void service() throws Exception {
        // do nothing
      }
    };

    assertEquals(State.NEW, service.state());
    Future<State> startState = service.start();
    startLatch.await();
    
    Future<State> stopState = service.stop();
    assertEquals(State.STARTING, service.state());    
    stopLatch.countDown();

    try {
      assertEquals(State.TERMINATED, startState.get());
      assertEquals(State.TERMINATED, stopState.get());
    } catch (Throwable th) {
      fail();
    }
  }
  
  @Test public void onStartCalled() throws ExecutionException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(0);
    Service service = new AbstractThreadService("test") {
      @Override protected void onStart() throws Exception {
        counter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        Thread.sleep(1000);
      }
    };
    
    assertEquals(State.NEW, service.state());
    service.start().get();
    assertEquals(State.RUNNING, service.state());
    assertEquals(1, counter.get());
  }
  
  @Test public void onStopCalled() throws ExecutionException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(0);
    Service service = new AbstractThreadService("test") {
      @Override protected void onStop() throws Exception {
        counter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        Thread.sleep(Long.MAX_VALUE);
      }
    };
    
    assertEquals(State.NEW, service.state());
    service.start().get();
    assertEquals(State.RUNNING, service.state());
    service.stop().get();
    assertEquals(1, counter.get());
  }
  
  @Test public void executionExceptionCheckState() throws Exception {
    Service service = new AbstractThreadService("test") {
      @Override protected void onStop() throws Exception {
        Thread.sleep(Long.MAX_VALUE);
      }

      @Override protected void service() throws Exception {
        throw new RuntimeException("This service always throws exception "
            + "when calling service() method.");
      }
    };
    
    service.start().get();
    assertEquals(State.RUNNING, service.state());
  }
  
  @Test public void interruptCalled() throws ExecutionException, InterruptedException {
    final AtomicInteger onStartCounter = new AtomicInteger(0);
    final AtomicInteger serviceCounter = new AtomicInteger(0);
    
    AbstractThreadService service = new AbstractThreadService("test") {
      @Override protected void onStart() throws Exception {
        onStartCounter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        serviceCounter.incrementAndGet();
        Thread.sleep(Long.MAX_VALUE);
      }
    };
    
    assertEquals(State.NEW, service.state());
    service.start().get();
    assertEquals(State.RUNNING, service.state());
    service.interrupt();
    Thread.sleep(100);
    
    assertEquals(1, onStartCounter.get());
    assertEquals(2, serviceCounter.get());
  }
  
  @Test public void serviceCalledException() throws ExecutionException, InterruptedException {
    final AtomicInteger onStartCounter = new AtomicInteger(0);
    final AtomicInteger onStopCounter = new AtomicInteger(0);
    final AtomicInteger serviceCounter = new AtomicInteger(0);
    final AtomicBoolean firstTime = new AtomicBoolean(true);
    Service service = new AbstractThreadService("test") {
      @Override protected void onStart() throws Exception {
        onStartCounter.incrementAndGet();
      }

      @Override protected void onStop() throws Exception {
        onStopCounter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        serviceCounter.incrementAndGet();
        if (firstTime.get()) {
          firstTime.set(false);
          throw new RuntimeException("This service always throws exception "
              + "when calling service() method.");
        } else {
          Thread.sleep(Long.MAX_VALUE);          
        }
      }
    };
    service.start().get();
    assertEquals(State.RUNNING, service.state());
    Thread.sleep(100);
    assertEquals(2, onStartCounter.get());
    assertEquals(1, onStopCounter.get());
    assertEquals(2, serviceCounter.get());
  }
  
  @Test public void unexpectedException() throws ExecutionException, InterruptedException {
    Service service = new AbstractThreadService("test") {
      @Override protected void onStop() throws Exception {
        throw new RuntimeException("This service always throws exception "
              + "when calling onStop() method.");
      }

      @Override protected void service() throws Exception {
        throw new AssertionError("This service always throws error "
              + "when calling service() method.");
      }
    };
    service.start().get();
    assertEquals(State.RUNNING, service.state());
    Thread.sleep(100);
    try {
      service.stop().get(1000, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      fail("TimeoutException has been thrown. This mean that we could not correctly stop service.");
    } catch (ExecutionException ex) {
      // do nothing
    }
  }
}
