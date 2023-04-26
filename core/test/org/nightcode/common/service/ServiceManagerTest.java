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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link ServiceManager}.
 */
public class ServiceManagerTest {

  @Test public void addShutdownHook() {
    ServiceManager serviceManager = ServiceManager.instance();
    assertNotNull(serviceManager);
    assertEquals(serviceManager, ServiceManager.instance());
  }

  @Test public void addShutdownHookNull() {
    ServiceManager serviceManager = new ServiceManager();
    try {
      serviceManager.addShutdownHook(null);
      fail("addShutdownHook must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("service", ex.getMessage());
    }
  }

  @Test public void addShutdownHookTwice() {
    Service serviceMock = EasyMock.createMock(Service.class);
    EasyMock.expect(serviceMock.serviceName()).andReturn("serviceMock").times(4);
 
    EasyMock.replay(serviceMock);

    ServiceManager serviceManager = new ServiceManager();
    serviceManager.addShutdownHook(serviceMock);
    try {
      serviceManager.addShutdownHook(serviceMock);
      fail("addShutdownHook must throw IllegalStateException");
    } catch (IllegalStateException ex) {
      assertEquals("service <serviceMock> has already been added", ex.getMessage());
    }

    EasyMock.verify(serviceMock);
  }

  @Test public void removeShutdownHookNull() {
    ServiceManager serviceManager = new ServiceManager();
    try {
      serviceManager.removeShutdownHook(null);
      fail("removeShutdownHook must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("service", ex.getMessage());
    }
  }

  @Test public void shutdownAll() throws ExecutionException, InterruptedException {
    CompletableFuture<Service.State> stateFuture = new CompletableFuture<>();
    Service serviceMock = new AbstractService("TestService") {
      @Override protected void doStart() {
        doStart();
      }

      @Override protected void doStop() {
        stopped();
      }

      @Override public CompletableFuture<State> stop() {
        CompletableFuture<Service.State> cf = super.stop();
        try {
          stateFuture.complete(cf.get());
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
        return stateFuture;
      }
    };
 
    ServiceManager serviceManager = new ServiceManager();
    serviceManager.addShutdownHook(serviceMock);
    serviceManager.shutdownAll();

    Assert.assertEquals(stateFuture.get(), Service.State.TERMINATED);
  }

  @Test public void shutdownAllWithTimeout() throws ExecutionException, TimeoutException, InterruptedException {
    CompletableFuture<Service.State> stateFuture = new CompletableFuture<>();
    Service serviceMock = new AbstractService("TestService") {
      @Override protected void doStart() {
        doStart();
      }

      @Override protected void doStop() {
        stopped();
      }

      @Override public CompletableFuture<State> stop() {
        CompletableFuture<Service.State> cf = super.stop();
        try {
          stateFuture.complete(cf.get());
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
        return stateFuture;
      }
    };
 
    ServiceManager serviceManager = new ServiceManager();
    serviceManager.addShutdownHook(serviceMock);
    serviceManager.shutdownAll(10 * 1000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(stateFuture.get(), Service.State.TERMINATED);
  }
}
