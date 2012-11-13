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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An object that divides CharSequence into a map of key/value pairs.
 */
public final class Splitter {

  private interface Matcher {
    boolean matches(char c);
  }

  private static final class CharMatcher implements Matcher {

    private final char c;

    private CharMatcher(char c) {
      this.c = c;
    }

    public boolean matches(char c) {
      return this.c == c;
    }
  }

  private static final class CharsMatcher implements Matcher {

    private final char[] chars;

    private CharsMatcher(char[] chars) {
      this.chars = chars;
    }

    @Override public boolean matches(char c) {
      return Arrays.binarySearch(chars, c) >= 0;
    }
  }

  private static final Matcher NONE = new Matcher() {
    @Override public boolean matches(char c) {
      return false;
    }
  };

  /**
   * Returns a splitter that uses the given fixed
   * string as a separator of key/value pair.
   *
   * @param pairSeparator separator of key/value pair
   * @return a splitter with the desired configuration
   */
  public static Splitter on(String pairSeparator) {
    return new Splitter(pairSeparator, "=", NONE);
  }

  private final String keyValueSeparator;
  private final String pairSeparator;
  private final Matcher trimMatcher;

  private Splitter(String pairSeparator, String keyValueSeparator, Matcher trimMatcher) {
    this.pairSeparator = pairSeparator;
    this.keyValueSeparator = keyValueSeparator;
    this.trimMatcher = trimMatcher;
  }

  /**
   * Splits the CharSequence passed in parameter.
   *
   * @param source the sequence of characters to split
   * @return a map of key/value pairs
   */
  public Map<String, String> split(final CharSequence source) {
    Objects.nonNull(source, "source");
    Map<String, String> parameters = new HashMap<String, String>();
    Iterator<String> i = new StringIterator(source, pairSeparator);
    while (i.hasNext()) {
      String keyValue = i.next();

      int keyValueSeparatorPosition = keyValueSeparatorStart(keyValue);
      if (keyValueSeparatorPosition == 0 || keyValue.length() == 0) {
        continue;
      }
      if (keyValueSeparatorPosition < 0) {
        parameters.put(keyValue, null);
        continue;
      }

      int valueStart = keyValueSeparatorPosition + keyValueSeparator.length();
      int valueEnd = keyValue.length();

      while (valueStart < valueEnd && trimMatcher.matches(keyValue.charAt(valueStart))) {
        valueStart++;
      }

      while (valueStart < valueEnd && trimMatcher.matches(keyValue.charAt(valueEnd - 1))) {
        valueEnd--;
      }

      String key = keyValue.substring(0, keyValueSeparatorPosition);
      String value = keyValue.substring(valueStart, valueEnd);
      parameters.put(key, value);
    }
    return parameters;
  }

  /**
   * Returns a splitter that removes all leading or trailing characters
   * matching the given character from each returned value.
   *
   * @param c character
   * @return a splitter with the desired configuration
   */
  public Splitter trimValues(char c) {
    return new Splitter(pairSeparator, keyValueSeparator, new CharMatcher(c));
  }

  /**
   * Returns a splitter that removes all leading or trailing characters
   * matching the given characters from each returned value.
   *
   * @param chars characters
   * @return a splitter with the desired configuration
   */
  public Splitter trimValues(char[] chars) {
    return new Splitter(pairSeparator, keyValueSeparator, new CharsMatcher(chars));
  }

  /**
   * Returns a splitter that behaves equivalently to this
   * splitter, but uses the given fixed string as a key/value separator.
   *
   * @param keyValueSeparator key/value separator
   * @return a splitter with the desired configuration
   */
  public Splitter withKeyValueSeparator(String keyValueSeparator) {
    return new Splitter(pairSeparator, keyValueSeparator, trimMatcher);
  }

  private boolean isKeyValueSeparator(String source, int position) {
    for (int i = 1; i < keyValueSeparator.length(); i++) {
      if (source.charAt(i + position) != keyValueSeparator.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private int keyValueSeparatorStart(String source) {
    for (int i = 0, j = source.length() - keyValueSeparator.length(); i <= j; i++) {
      if (source.charAt(i) == keyValueSeparator.charAt(0) && isKeyValueSeparator(source, i)) {
        return i;
      }
    }
    return -1;
  }
}
