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

import org.nightcode.common.service.Service.State;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nightcode.common.service.Service.State.FAILED;
import static org.nightcode.common.service.Service.State.NEW;
import static org.nightcode.common.service.Service.State.RUNNING;
import static org.nightcode.common.service.Service.State.TERMINATED;

/**
 * Unit test for {@link AbstractService}.
 */
public class AbstractServiceTest {

  @Test public void getServiceName() {
    Service service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }

      @Override public String serviceName() {
        return "ServiceTest";
      }
    };
    assertEquals("ServiceTest", service.serviceName());
  }
  
  @Test public void toStringPrint() {
    Service service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }

      @Override public String serviceName() {
        return "ServiceTest";
      }
    };
    assertEquals("ServiceTest[" + NEW + "]", service.toString());
    service.startAsync();
    assertEquals("ServiceTest[" + State.RUNNING + "]", service.toString());
    service.stopAsync();
    assertEquals("ServiceTest[" + State.TERMINATED + "]", service.toString());
  }
  
  @Test public void startAsyncCalled() throws ExecutionException, InterruptedException {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }
    };
    
    assertEquals(NEW, service.state.get());
    service.startAsync();
    assertEquals(RUNNING, service.state.get());
  }

  @Test public void startAsyncCalledException() {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        throw new RuntimeException("This service always throws exception when calling doStart() method.");
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception when calling doStop() method.");
      }
    };

    assertEquals(NEW, service.state.get());
    try {
      service.startAsync().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception when calling doStart() method."));
      assertEquals(FAILED, service.state.get());
      return;
    }
    fail();
  }

  @Test public void startAsyncCalledExceptionCheckStopFuture() {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        throw new RuntimeException("This service always throws exception when calling doStart() method.");
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception when calling doStop() method.");
      }
    };

    assertEquals(NEW, service.state.get());
    try {
      service.startAsync().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception when calling doStart() method."));
      assertEquals(FAILED, service.state.get());

      try {
        service.stopAsync().get();
      } catch (Throwable t) {
        assertTrue(th.getMessage().contains("This service always throws exception when calling doStart() method."));
        assertEquals(FAILED, service.state.get());
        return;
      }
    }
    fail();
  }

  @Test public void stopCalled() {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }
    };
    assertEquals(NEW, service.state.get());
    service.startAsync();
    service.stopAsync();
    assertEquals(TERMINATED, service.state.get());
  }
  
  @Test public void stopNewAbstractServiceCalled() throws ExecutionException, InterruptedException {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }
    };
    assertEquals(NEW, service.state.get());
    service.stopAsync();
    assertEquals(TERMINATED, service.state.get());
    assertEquals(State.TERMINATED, service.startAsync().get().state());
    assertEquals(State.TERMINATED, service.stopAsync().get().state());
  }
  
  @Test public void stopCalledException()
      throws ExecutionException, InterruptedException {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        throw new RuntimeException("This service always throws exception when calling doStop() method.");
      }
    };

    assertEquals(NEW, service.state.get());
    service.startAsync();
    assertEquals(RUNNING, service.state.get());
    try {
      service.stopAsync().get();
    } catch (Throwable th) {
      assertTrue(th.getMessage().contains("This service always throws exception when calling doStop() method."));
      assertEquals(FAILED, service.state.get());
      return;
    }
    fail();
  }
  
  @Test public void notifyFailedNull() {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }
    };
    try {
      service.notifyFailed(null);
      fail("serviceFailed must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("cause", ex.getMessage());
    }
  }
  
  @Test public void notifyStartedState() {
    AbstractService service = new AbstractService() {
      @Override protected void doStart() {
        notifyStarted();
      }

      @Override protected void doStop() {
        notifyStopped();
      }
    };
    try {
      service.notifyStarted();
      fail("started must throw IllegalStateException");
    } catch (IllegalStateException ex) {
      assertEquals("cannot notifyStarted() when the service is NEW", ex.getMessage());
    }
  }
}
