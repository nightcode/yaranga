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

package org.nightcode.common.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

import org.nightcode.common.scheduler.RetryConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ReflectUtils}.
 */
public class ReflectUtilsTest {

  private static final class TestClass {
    public CompletableFuture<Boolean> test() {
      return CompletableFuture.completedFuture(true);
    }
  }

  @Test public void isClassPresent() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    assertTrue(ReflectUtils.isClassPresent(ReflectUtils.class.getName(), cl));
    assertFalse(ReflectUtils.isClassPresent("com.example.Test", cl));
  }

  @Test public void findMethod() {
    Method method = ReflectUtils.findMethod(RetryConfig.class, "toString");
    String result = (String) ReflectUtils.invokeMethod(method, new RetryConfig(10, 10));
    assertEquals("RetryConfig{minDelayMs=10, maxDelayMs=10}", result);
  }

  @Test public void resolveType() {
    Method method = ReflectUtils.findMethod(TestClass.class, "test");
    
    Type type = ReflectUtils.resolveType(method.getGenericReturnType());
    assertTrue(type instanceof ParameterizedType parameterized && parameterized.getRawType().equals(CompletableFuture.class));
    assertEquals(Boolean.class, ((ParameterizedType) type).getActualTypeArguments()[0]);
  }
}
