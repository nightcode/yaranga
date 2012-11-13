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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link Hexs}. 
 */
public class HexsTest {

  private static final String HEX_STRING_LC = "00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f";
  private static final String HEX_STRING_UC = "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F";
  private static final byte[] EXPECTED_BYTE_ARRAY 
      = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf};
  
  @Test public void hex() {
    final String hexString = HEX_STRING_UC.replace(" ", "");
    Hexs hexs = Hexs.hex();
    byte[] actualByteArray = hexs.toByteArray(hexString);
    assertArrayEquals(EXPECTED_BYTE_ARRAY, actualByteArray);
    assertEquals(hexString, hexs.fromByteArray(actualByteArray));
  }

  @Test public void hexLowerCase() {
    final String hexString = HEX_STRING_LC.replace(" ", "");
    Hexs hexs = Hexs.hex().lowerCase();
    byte[] actualByteArray = hexs.toByteArray(hexString);
    assertArrayEquals(EXPECTED_BYTE_ARRAY, actualByteArray);
    assertEquals(hexString, hexs.fromByteArray(actualByteArray));
  }
  
  @Test public void hexWithByteSeparator() {
    final String hexString = HEX_STRING_UC.replace(" ", "");
    Hexs hexs = Hexs.hex().withByteSeparator(" ");
    byte[] hexByteArray = hexs.toByteArray(hexString);
    assertEquals(HEX_STRING_UC, hexs.fromByteArray(hexByteArray));
  }
  
  @Test public void hexLowerCaseWithByteSeparator() {
    final String hexString = HEX_STRING_LC.replace(" ", "");
    Hexs hexs = Hexs.hex().lowerCase().withByteSeparator(" ");
    byte[] hexByteArray = hexs.toByteArray(hexString);
    assertEquals(HEX_STRING_LC, hexs.fromByteArray(hexByteArray));
  }
  
  @Test public void hexWithOffset(){
    int offset = 2;
    int length = 4;
    final String hexString = HEX_STRING_UC.replace(" ", "");
    final String expectedHexString = hexString.substring(offset * 2, (offset + length) * 2);
    Hexs hexs = Hexs.hex();
    byte[] actualByteArray = hexs.toByteArray(hexString);
    assertArrayEquals(EXPECTED_BYTE_ARRAY, actualByteArray);
    assertEquals(expectedHexString, hexs.fromByteArray(actualByteArray, offset, length));
  }
  
  @Test public void hexNullByteArray() {
    Hexs hexs = Hexs.hex();
    try {
      hexs.fromByteArray(null);
      fail("fromByteArray must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("bytes", ex.getMessage());
    }

    try {
      hexs.fromByteArray(null, 0, 1);
      fail("fromByteArray must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("bytes", ex.getMessage());
    }
  }
  
  @Test public void hexNullHexString() {
    Hexs hexs = Hexs.hex();
    try {
      hexs.toByteArray(null);
      fail("toByteArray must throw NullPointerException");
    } catch (NullPointerException ex) {
      assertEquals("hexadecimal string", ex.getMessage());
    }
  }
  
  @Test public void hexOffsetLength() {
    Hexs hexs = Hexs.hex();
    final String hexString = HEX_STRING_UC.replace(" ", "");
    byte[] byteArray = hexs.toByteArray(hexString);
    try {
      hexs.fromByteArray(byteArray, -1, 1);
      fail("fromByteArray must throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("offset must be equal or greater than zero", ex.getMessage());
    }
    try {
      hexs.fromByteArray(byteArray, 0, -1);
      fail("fromByteArray must throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("length must be greater than zero", ex.getMessage());
    }
    try {
      hexs.fromByteArray(byteArray, 3, byteArray.length);
      fail("fromByteArray must throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("(offset + length) must be less than " + byteArray.length, ex.getMessage());
    }
  }
  
  @Test public void hexPrecondition() {
    Hexs hexs = Hexs.hex();
    try {
      hexs.toByteArray("ABC");
      fail("toByteArray must throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertEquals("hexadecimal string <ABC> must have an even number of characters."
          , ex.getMessage());
    }
  }
}
