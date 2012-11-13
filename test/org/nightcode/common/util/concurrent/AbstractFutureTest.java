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

package org.nightcode.common.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link AbstractFuture}.
 */
public class AbstractFutureTest {
  
  @Test public void succeeded() throws ExecutionException, InterruptedException {
    AbstractFuture<Boolean> future = new AbstractFuture<Boolean>() {};
    future.succeeded(Boolean.TRUE);
    assertEquals(Boolean.TRUE, future.get());
  }
  
  @Test public void succeededTimeout()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractFuture<Boolean> future = new AbstractFuture<Boolean>() {};
    future.succeeded(Boolean.TRUE);
    assertEquals(Boolean.TRUE, future.get(5, TimeUnit.MILLISECONDS));
  }
  
  @Test public void failed() {
    AbstractFuture<Boolean> future = new AbstractFuture<Boolean>() {};
    future.failed(new Exception("AbstractFuture failed() exception."));
    try {
      future.get();
    } catch (Exception ex) {
      assertTrue(ex.getMessage().contains("AbstractFuture failed() exception."));
      return;
    }
    fail();
  }
  
  @Test public void get() throws ExecutionException, InterruptedException {
    AbstractFuture<Boolean> future = new AbstractFuture<Boolean>() {};
    try {
      future.get(5, TimeUnit.MILLISECONDS);
    } catch (Exception ex) {
      assertTrue(ex instanceof TimeoutException);
      return;
    }
    fail();
  }
  
  @Test public void done() {
    AbstractFuture<Boolean> future = new AbstractFuture<Boolean>() {};
    assertEquals(false, future.isDone());
    future.succeeded(Boolean.TRUE);
    assertEquals(true, future.isDone());
  }
  
  @Test public void cancel() {
    AbstractFuture<Boolean> future = new AbstractFuture<Boolean>() {};
    assertEquals(false, future.isCancelled());
    assertEquals(false, future.cancel(true));
  }
}
