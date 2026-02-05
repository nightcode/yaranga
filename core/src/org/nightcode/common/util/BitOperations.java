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

/**
 * Bit operations.
 */
public enum BitOperations {
  ;

  private static final String ARRAYS_ERROR_TEMPLATE = "left and right arrays has different length [%s, %s]";

  public static byte[] and(final byte[] left, final byte[] right) {
    if (left.length != right.length) {
      throw new IllegalStateException(String.format(ARRAYS_ERROR_TEMPLATE, left.length, right.length));
    }

    int    size = left.length;
    byte[] buf  = new byte[size];
    for (int i = 0; i < size; i++) {
      buf[i] = (byte) (left[i] & right[i]);
    }
    return buf;
  }

  public static boolean isSet(final byte value, final int bitPosition) {
    if (bitPosition < 0 || bitPosition > 7) {
      throw new IllegalArgumentException("bitPosition should be grate than -1 and less than 8, current value: " + bitPosition);
    }
    return ((value & 0xFF) & (1 << bitPosition)) != 0;
  }

  public static boolean isSet(final int value, final int bitPosition) {
    if (bitPosition < 0 || bitPosition > 31) {
      throw new IllegalArgumentException("bitPosition should be grate than -1 and less than 32, current value: " + bitPosition);
    }
    return (value & (1 << bitPosition)) != 0;
  }

  public static boolean isSet(final long value, final int bitPosition) {
    if (bitPosition < 0 || bitPosition > 63) {
      throw new IllegalArgumentException("bitPosition should be grate than -1 and less than 64, current value: " + bitPosition);
    }
    return (value & (1L << bitPosition)) != 0;
  }

  public static byte[] leftShift(final byte[] src, int numberOfBits) {
    if (numberOfBits > 8) {
      throw new IllegalArgumentException("number of bits for shift must be less or equals than 8");
    }
    final int skipBits = 8 - numberOfBits;
    byte[]    buffer   = new byte[src.length];
    byte      shift    = 0;
    for (int i = src.length - 1; i >= 0; i--) {
      buffer[i] = (byte) (((src[i] << numberOfBits) & 0xFF) | shift);
      shift     = (byte) ((src[i] & 0xFF) >> skipBits);
    }
    return buffer;
  }

  public static byte[] or(final byte[] left, final byte[] right) {
    if (left.length != right.length) {
      throw new IllegalStateException(String.format(ARRAYS_ERROR_TEMPLATE, left.length, right.length));
    }

    int    size = left.length;
    byte[] buf  = new byte[size];
    for (int i = 0; i < size; i++) {
      buf[i] = (byte) (left[i] | right[i]);
    }
    return buf;
  }

  public static byte[] xor(final byte[] left, final byte[] right) {
    if (left.length != right.length) {
      throw new IllegalStateException(String.format(ARRAYS_ERROR_TEMPLATE, left.length, right.length));
    }

    int    size = left.length;
    byte[] buf  = new byte[size];
    for (int i = 0; i < size; i++) {
      buf[i] = (byte) (left[i] ^ right[i]);
    }
    return buf;
  }

  public static byte[] xor(final byte[] left, final byte[] right, final int rightOffset, final int rightLength) {
    if (left.length != rightLength) {
      throw new IllegalStateException(String.format(ARRAYS_ERROR_TEMPLATE, left.length, right.length));
    }

    int    size = left.length;
    byte[] buf  = new byte[size];
    for (int i = 0; i < size; i++) {
      buf[i] = (byte) (left[i] ^ right[i + rightOffset]);
    }
    return buf;
  }
}
