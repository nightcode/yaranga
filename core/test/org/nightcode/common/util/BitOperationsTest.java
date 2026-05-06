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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link BitOperations}.
 */
public class BitOperationsTest {

  @Test public void and() {
    byte[] left  = new byte[]{0x10, 0x01, 0x10, 0x00, 0x11};
    byte[] right = new byte[]{0x01, 0x10, 0x01, 0x00, 0x11};

    byte[] result = BitOperations.and(left, right);
    assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x11}, result);
  }

  @Test public void or() {
    byte[] left  = new byte[]{0x10, 0x01, 0x10, 0x00, 0x11};
    byte[] right = new byte[]{0x01, 0x10, 0x01, 0x00, 0x11};

    byte[] result = BitOperations.or(left, right);
    assertArrayEquals(new byte[]{0x11, 0x11, 0x11, 0x00, 0x11}, result);
  }

  @Test public void xor() {
    byte[] left  = new byte[]{0x10, 0x01, 0x10, 0x00, 0x11};
    byte[] right = new byte[]{0x01, 0x10, 0x01, 0x00, 0x11};

    byte[] result = BitOperations.xor(left, right);
    assertArrayEquals(new byte[]{0x11, 0x11, 0x11, 0x00, 0x00}, result);
  }

  @Test public void xorWithOffset() {
    byte[] left  = new byte[]{0x10, 0x01, 0x10, 0x00, 0x11};
    byte[] right = new byte[]{0x00, 0x01, 0x10, 0x01, 0x00, 0x11, 0x00};

    byte[] result = BitOperations.xor(left, right, 1, 5);
    assertArrayEquals(new byte[]{0x11, 0x11, 0x11, 0x00, 0x00}, result);
  }

  @Test public void leftShift() {
    byte[] src  = new byte[]{0x10, 0x01, 0x10, 0x00, 0x11};

    byte[] result = BitOperations.leftShift(src, 1);
    assertArrayEquals(new byte[]{0x20, 0x02, 0x20, 0x00, 0x22}, result);
  }

  @Test public void isSet() {
    assertTrue(BitOperations.isSet((byte) 0x80, 7));
    try {
      BitOperations.isSet((byte) 0x80, 8);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("bitPosition should be grate than -1 and less than 8, current value: 8", ex.getMessage());
    }

    assertTrue(BitOperations.isSet(0x80000000, 31));
    try {
      BitOperations.isSet(0x80000000, 32);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("bitPosition should be grate than -1 and less than 32, current value: 32", ex.getMessage());
    }

    assertTrue(BitOperations.isSet(0x8000000000000000L, 63));
    try {
      BitOperations.isSet(0x8000000000000000L, 64);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("bitPosition should be grate than -1 and less than 64, current value: 64", ex.getMessage());
    }
  }
}
