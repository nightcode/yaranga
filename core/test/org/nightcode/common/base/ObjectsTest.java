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

package org.nightcode.common.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link Objects}.
 */
public class ObjectsTest {
  
  @Test public void validArgument() {
    String message = "error message";
    try {
      Objects.validArgument(false, message);
    } catch (IllegalArgumentException ex) {
      assertEquals(message, ex.getMessage());
    }
    
    Objects.validArgument(true, message);
  }

  @Test public void validArgumentMessage() {
    String message = "error %s";
    String argument = "message";
    try {
      Objects.validArgument(false, message, argument);
    } catch (IllegalArgumentException ex) {
      assertEquals("error message", ex.getMessage());
    }

    Objects.validArgument(true, message, argument);
  }

  @Test public void validState() {
    String message = "error message";
    try {
      Objects.validState(false, message);
    } catch (IllegalStateException ex) {
      assertEquals(message, ex.getMessage());
    }

    Objects.validState(true, message);
  }

  @Test public void validStateMessage() {
    String message = "error %s";
    String argument = "message";
    try {
      Objects.validState(false, message, argument);
    } catch (IllegalStateException ex) {
      assertEquals("error message", ex.getMessage());
    }

    Objects.validState(true, message, argument);
  }
}
