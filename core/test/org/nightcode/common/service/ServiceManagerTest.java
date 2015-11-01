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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.easymock.EasyMock;
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
    EasyMock.expect(serviceMock.serviceName()).andReturn("serviceMock").times(3);
 
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

  @SuppressWarnings("unchecked")
  @Test public void shutdownAll()
      throws ExecutionException, TimeoutException, InterruptedException {
    Future<Service.State> stateFutureMock = EasyMock.createMock(Future.class);

    Service serviceMock = EasyMock.createStrictMock(Service.class);
    EasyMock.expect(serviceMock.serviceName()).andReturn("serviceMock").times(2);
    EasyMock.expect(serviceMock.stop()).andReturn(stateFutureMock).once();
    EasyMock.expect(stateFutureMock.get()).andReturn(Service.State.TERMINATED).once();
    EasyMock.replay(serviceMock);
 
    ServiceManager serviceManager = new ServiceManager();
    serviceManager.addShutdownHook(serviceMock);
    serviceManager.shutdownAll();

    EasyMock.verify(serviceMock);
  }

  @SuppressWarnings("unchecked")
  @Test public void shutdownAllWithTimeout()
      throws ExecutionException, TimeoutException, InterruptedException {
    Future<Service.State> stateFutureMock = EasyMock.createMock(Future.class);

    Service serviceMock = EasyMock.createStrictMock(Service.class);
    EasyMock.expect(serviceMock.serviceName()).andReturn("serviceMock").times(2);
    EasyMock.expect(serviceMock.stop()).andReturn(stateFutureMock).once();
    EasyMock.expect(stateFutureMock.get(10 * 1000, TimeUnit.MILLISECONDS))
        .andReturn(Service.State.TERMINATED).once();
    EasyMock.replay(serviceMock);
 
    ServiceManager serviceManager = new ServiceManager();
    serviceManager.addShutdownHook(serviceMock);
    serviceManager.shutdownAll(10 * 1000, TimeUnit.MILLISECONDS);

    EasyMock.verify(serviceMock);
  }
}
