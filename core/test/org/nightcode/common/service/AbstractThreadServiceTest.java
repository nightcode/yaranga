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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.nightcode.common.service.Service.State;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.nightcode.common.service.Service.State.FAILED;
import static org.nightcode.common.service.Service.State.NEW;
import static org.nightcode.common.service.Service.State.RUNNING;
import static org.nightcode.common.service.Service.State.STARTING;
import static org.nightcode.common.service.Service.State.TERMINATED;

/**
 * Unit test for {@link AbstractThreadService}.
 */
public class AbstractThreadServiceTest {
    
  @Test public void getServiceName() {
    Service service = new AbstractThreadService() {
      @Override protected void service() {
        // do nothing
      }

      @Override public String serviceName() {
        return "ThreadServiceTest";
      }
    };
    assertEquals("ThreadServiceTest", service.serviceName());
  }
    
  @Test public void toStringPrint() throws ExecutionException, InterruptedException {
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void service() throws Exception {
        Thread.sleep(Integer.MAX_VALUE);
      }

      @Override public String serviceName() {
        return "ThreadServiceTest";
      }
    };
    assertEquals("ThreadServiceTest[" + NEW + "]", service.toString());
    service.startAsync().get();
    assertEquals("ThreadServiceTest[" + State.RUNNING + "]", service.toString());
    service.stopAsync().get();
    assertEquals("ThreadServiceTest[" + TERMINATED + "]", service.toString());
  }
  
  @Test public void startAsyncCalled() throws ExecutionException, InterruptedException {
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void service() throws Exception {
        Thread.sleep(Integer.MAX_VALUE);
      }
    };
    
    assertEquals(NEW, service.state());
    service.startAsync().get();
    assertEquals(State.RUNNING, service.state());
  }
  
  @Test public void startAsyncCalledException() {
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStart() {
        throw new RuntimeException("This service always throws exception when calling onStart() method.");
      }

      @Override protected void service() {
        throw new AssertionError();
      }
    };
    
    assertEquals(NEW, service.state());
    try {
      service.startAsync().get();
      fail("should throw exception");
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception when calling onStart() method."));
      assertEquals(FAILED, service.state());
    }
  }

  @Test public void startAsyncCalledExceptionCheckStopFuture() {
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStart() {
        throw new RuntimeException("This service always throws exception when calling onStart() method.");
      }

      @Override protected void service() {
        throw new AssertionError();
      }
    };

    assertEquals(NEW, service.state());
    try {
      service.startAsync().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception when calling onStart() method."));
      assertEquals(FAILED, service.state());

      try {
        service.stopAsync().get();
        fail("should throw exception");
      } catch (Throwable t) {
        assertTrue(th.getMessage().contains("This service always throws exception when calling onStart() method."));
        assertEquals(FAILED, service.state());
      }
    }
  }
  
  @Test public void stopCalled() throws ExecutionException, InterruptedException {
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void service() {
        // do nothing
      }
    };
    assertEquals(NEW, service.state());
    service.startAsync().get();
    service.stopAsync().get();
    assertEquals(TERMINATED, service.state());
  }
  
  @Test public void stopCalledExceptionIllegalState() throws InterruptedException {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch stopLatch = new CountDownLatch(1);

    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStart() {
        startLatch.countDown();
        try {
          stopLatch.await();
        } catch (InterruptedException ignore) {
          // do nothing
        }
      }

      @Override protected void service() {
        // do nothing
      }
    };

    assertEquals(NEW, service.state());
    Future<Service> startState = service.startAsync();
    startLatch.await();
    
    Future<Service> stopState = service.stopAsync();
    assertEquals(STARTING, service.state());    
    stopLatch.countDown();

    try {
      assertEquals(TERMINATED, startState.get().state());
      assertEquals(TERMINATED, stopState.get().state());
    } catch (Throwable th) {
      fail();
    }
  }
  
  @Test public void onStartAsyncCalled() throws ExecutionException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(0);
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStart() {
        counter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        Thread.sleep(1000);
      }
    };
    
    assertEquals(NEW, service.state());
    service.startAsync().get();
    assertEquals(RUNNING, service.state());
    assertEquals(1, counter.get());
  }
  
  @Test public void onStopCalled() throws ExecutionException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(0);
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStop() {
        counter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        Thread.sleep(Long.MAX_VALUE);
      }
    };
    
    assertEquals(NEW, service.state());
    service.startAsync().get();
    assertEquals(RUNNING, service.state());
    service.stopAsync().get();
    assertEquals(1, counter.get());
  }
  
  @Test public void executionExceptionCheckState() throws Exception {
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStop() throws Exception {
        Thread.sleep(Long.MAX_VALUE);
      }

      @Override protected void service() {
        throw new RuntimeException("This service always throws exception when calling service() method.");
      }
    };
    
    service.startAsync().get();
    assertEquals(RUNNING, service.state());
  }
  
  @Test public void interruptCalled() throws ExecutionException, InterruptedException {
    final AtomicInteger onStartCounter = new AtomicInteger(0);
    final AtomicInteger onStopCounter = new AtomicInteger(0);
    final AtomicInteger serviceCounter = new AtomicInteger(0);
    
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStart() {
        onStartCounter.incrementAndGet();
      }

      @Override protected void onStop() {
        onStopCounter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        serviceCounter.incrementAndGet();
        Thread.sleep(Long.MAX_VALUE);
      }
    };

    assertEquals(NEW, service.state());
    service.startAsync().get();
    assertEquals(RUNNING, service.state());
    service.interrupt();
    Thread.sleep(100);

    assertEquals(1, onStartCounter.get());
    assertEquals(1, onStopCounter.get());
    assertEquals(1, serviceCounter.get());
  }

  @Test public void serviceCalledException() throws ExecutionException, InterruptedException {
    final AtomicInteger onStartCounter = new AtomicInteger(0);
    final AtomicInteger onStopCounter = new AtomicInteger(0);
    final AtomicInteger serviceCounter = new AtomicInteger(0);
    final AtomicBoolean firstTime = new AtomicBoolean(true);
    AbstractThreadService service = new AbstractThreadService() {
      @Override protected void onStart() {
        onStartCounter.incrementAndGet();
      }

      @Override protected void onStop() {
        onStopCounter.incrementAndGet();
      }

      @Override protected void service() throws Exception {
        serviceCounter.incrementAndGet();
        if (firstTime.get()) {
          firstTime.set(false);
          throw new RuntimeException("This service always throws exception when calling service() method.");
        } else {
          Thread.sleep(Long.MAX_VALUE);          
        }
      }
    };
    service.startAsync().get();
    assertEquals(RUNNING, service.state());
    Thread.sleep(100);
    assertEquals(2, onStartCounter.get());
    assertEquals(1, onStopCounter.get());
    assertEquals(2, serviceCounter.get());
  }
  
  @Test public void unexpectedException() throws ExecutionException, InterruptedException {
    Service service = new AbstractThreadService() {
      @Override protected void onStop() {
        throw new RuntimeException("This service always throws exception when calling onStop() method.");
      }

      @Override protected void service() {
        throw new AssertionError("This service always throws error when calling service() method.");
      }
    };
    service.startAsync().get();
    Thread.sleep(100);
    try {
      service.stopAsync().get(1000, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      fail("TimeoutException has been thrown. This mean that we could not correctly stop service.");
    } catch (ExecutionException ex) {
      // do nothing
    }
  }
}
