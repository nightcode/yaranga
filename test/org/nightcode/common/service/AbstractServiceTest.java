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

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link AbstractService}.
 */
public class AbstractServiceTest {

  @Test public void getServiceName() {
    Service service = new AbstractService("ServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals("ServiceTest", service.serviceName());
  }
  
  @Test public void toStringPrint() {
    Service service = new AbstractService("ServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals("ServiceTest[" + State.NEW + "]", service.toString());
    service.start();
    assertEquals("ServiceTest[" + State.RUNNING + "]", service.toString());
    service.stop();
    assertEquals("ServiceTest[" + State.TERMINATED + "]", service.toString());
  }
  
  @Test public void startCalled() throws ExecutionException, InterruptedException {
    Service service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    
    assertEquals(State.NEW, service.state());
    service.start();
    assertEquals(State.RUNNING, service.state());
  }

  @Test public void startCalledException() {
    Service service = new AbstractService("test") {
      @Override protected void doStart() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStart() method.");
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStop() method.");
      }
    };

    assertEquals(State.NEW, service.state());
    try {
      service.start().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling doStart() method."));
      assertEquals(State.FAILED, service.state());
      return;
    }
    fail();
  }

  @Test public void startCalledExceptionCheckStopFuture() {
    Service service = new AbstractService("test") {
      @Override protected void doStart() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStart() method.");
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStop() method.");
      }
    };

    assertEquals(State.NEW, service.state());
    try {
      service.start().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling doStart() method."));
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

  @Test public void stopCalled() {
    Service service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals(State.NEW, service.state());
    service.start();
    service.stop();
    assertEquals(State.TERMINATED, service.state());
  }
  
  @Test public void stopNewAbstractServiceCalled() throws ExecutionException, InterruptedException {
    Service service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals(State.NEW, service.state());
    service.stop();
    assertEquals(State.TERMINATED, service.state());
    assertEquals(State.TERMINATED, service.start().get());
    assertEquals(State.TERMINATED, service.stop().get());
  }
  
  @Test public void stopCalledException()
      throws ExecutionException, InterruptedException {
    Service service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStop() method.");
      }
    };

    assertEquals(State.NEW, service.state());
    service.start();
    assertEquals(State.RUNNING, service.state());
    try {
      service.stop().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling doStop() method."));
      assertEquals(State.FAILED, service.state());
      return;
    }
    fail();
  }
  
  @Test public void setState() {
    AbstractService service = new AbstractService("ServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals(State.NEW, service.state());
    service.setState(State.FAILED);
    assertEquals(State.FAILED, service.state());
  }
  
  @Test public void serviceFailedNull() {
    AbstractService service = new AbstractService("ServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    try {
      service.serviceFailed(null);
      fail("serviceFailed must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("cause", ex.getMessage());
    }
  }
  
  @Test public void startedState() {
    AbstractService service = new AbstractService("ServiceTest") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    try {
      service.started();
      fail("started must throw IllegalStateException");
    } catch (IllegalStateException ex) {
      assertEquals("Cannot started service when it is NEW", ex.getMessage());
    }
  }
}
