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

package org.nightcode.common.lang;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ThrowingFunction}.
 */
public class ThrowingFunctionTest {

  @Test public void toFunction() {
    Function<Boolean, Boolean> actual = ThrowingFunction.toFunction((ThrowingFunction<Boolean, Boolean, Throwable>) b -> Boolean.TRUE);
    assertTrue(actual.apply(false));
  }
}
