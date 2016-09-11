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
    AbstractService service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    
    assertEquals(0x00, service.state.get());
    service.start();
    assertEquals(0x02, service.state.get());
  }

  @Test public void startCalledException() {
    AbstractService service = new AbstractService("test") {
      @Override protected void doStart() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStart() method.");
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStop() method.");
      }
    };

    assertEquals(0x00, service.state.get());
    try {
      service.start().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling doStart() method."));
      assertEquals(0x20, service.state.get());
      return;
    }
    fail();
  }

  @Test public void startCalledExceptionCheckStopFuture() {
    AbstractService service = new AbstractService("test") {
      @Override protected void doStart() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStart() method.");
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStop() method.");
      }
    };

    assertEquals(0x00, service.state.get());
    try {
      service.start().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling doStart() method."));
      assertEquals(0x20, service.state.get());

      try {
        service.stop().get();
      } catch (Throwable t) {
        assertTrue(t.getMessage().contains("service failed to start"));
        assertEquals(0x20, service.state.get());
        return;
      }
    }
    fail();
  }

  @Test public void stopCalled() {
    AbstractService service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals(0x00, service.state.get());
    service.start();
    service.stop();
    assertEquals(0x10, service.state.get());
  }
  
  @Test public void stopNewAbstractServiceCalled() throws ExecutionException, InterruptedException {
    AbstractService service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        stopped();
      }
    };
    assertEquals(0x00, service.state.get());
    service.stop();
    assertEquals(0x10, service.state.get());
    assertEquals(State.TERMINATED, service.start().get());
    assertEquals(State.TERMINATED, service.stop().get());
  }
  
  @Test public void stopCalledException()
      throws ExecutionException, InterruptedException {
    AbstractService service = new AbstractService("test") {
      @Override protected void doStart() {
        started();
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception "
            + "when calling doStop() method.");
      }
    };

    assertEquals(0x00, service.state.get());
    service.start();
    assertEquals(0x02, service.state.get());
    try {
      service.stop().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception "
          + "when calling doStop() method."));
      assertEquals(0x20, service.state.get());
      return;
    }
    fail();
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
      assertEquals("cannot start service when it is 0", ex.getMessage());
    }
  }
}
