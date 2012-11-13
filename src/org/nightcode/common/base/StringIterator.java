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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * String iterator.
 */
public final class StringIterator implements Iterator<String> {

  private final CharSequence source;
  private final int limit;
  private final String delimiter;
  private final int delimiterLength;
  private int offset = 0;

  public StringIterator(CharSequence source, String delimiter) {
    this.source = source;
    this.limit = source.length();
    this.delimiter = delimiter;
    this.delimiterLength = delimiter.length();
  }

  @Override public boolean hasNext() {
    return offset < limit;
  }

  @Override public String next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    int position = delimiterStart(offset);
    if (position < 0) {
      position = limit;
    }
    CharSequence value = source.subSequence(offset, position);
    offset = position + delimiterLength;
    return value.toString();
  }

  @Override public void remove() {
    throw new UnsupportedOperationException();
  }

  private int delimiterStart(int start) {
    for (int i = start, j = limit - delimiterLength; i <= j; i++) {
      if (source.charAt(i) == delimiter.charAt(0) && isDelimiter(i)) {
        return i;
      }
    }
    return -1;
  }

  private boolean isDelimiter(int offset) {
    for (int i = 1; i < delimiterLength; i++) {
      if (source.charAt(i + offset) != delimiter.charAt(i)) {
        return false;
      }
    }
    return true;
  }
}
