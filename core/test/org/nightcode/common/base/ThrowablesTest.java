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

package org.nightcode.common.base;

import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link Throwables}.
 */
public class ThrowablesTest {

  private static final class TestException extends Exception {

    public TestException(String message) {
      super(message, null, true, false);
    }
  }
  
  @Test public void testGetRootCause() {
    Throwable expected = new IOException();
    Throwable actual;
    try {
      Throwable throwable = new IllegalStateException(expected);
      throwable = new RuntimeException(throwable);
      throw throwable;
    } catch (Throwable ex) {
      actual = Throwables.getRootCause(ex);
    }
    assertEquals(expected, actual);
  }

  @Test public void testGetRootCauseLoop() throws NoSuchFieldException, IllegalAccessException {
    Throwable root = new IOException();
    Throwable chain = new IOException(new IOException(new IOException(new IOException(new IOException(root)))));
    Field causeField = Throwable.class.getDeclaredField("cause");
    try {
      causeField.setAccessible(true);
      causeField.set(root, chain);
      Throwables.getRootCause(chain);
      Assert.fail("must throw IllegalStateException");
    } catch (IllegalStateException ex) {
      Assert.assertEquals("loop in casual chain", ex.getMessage());
    } finally {
      causeField.setAccessible(false);
    }
  }

  @Test public void testGetStackTrace() {
    Exception exception = new TestException("message");
    String stackTrace = Throwables.getStackTrace(exception);
    Assert.assertEquals("org.nightcode.common.base.ThrowablesTest$TestException: message\n", stackTrace);
  }
}
