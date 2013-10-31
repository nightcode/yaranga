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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link StringIterator}.
 */
public class StringIteratorTest {
  
  @Test public void stringIterator() {
    final String str = "&a=b&c=\"d&e=\"f\"&d=&=e";
    final String expected = "|a=b|c=\"d|e=\"f\"|d=|=e";
    
    Iterator<String> i = new StringIterator(str, "&");
    StringBuilder actual = new StringBuilder();
    if (i.hasNext()) {
      actual.append(i.next());
      while(i.hasNext()) {
        actual.append('|').append(i.next());
      }
    }
    assertEquals(expected, actual.toString());
  }
  
  @Test public void stringIteratorComplexDelimiter() {
    final String str = "a=b&=c=\"d&=e=\"f\"&=d=&==e";
    final String expected = "a=b|c=\"d|e=\"f\"|d=|=e";
    
    Iterator<String> i = new StringIterator(str, "&=");
    StringBuilder actual = new StringBuilder();
    if (i.hasNext()) {
      actual.append(i.next());
      while(i.hasNext()) {
        actual.append('|').append(i.next());
      }
    }
    assertEquals(expected, actual.toString());
  }
  
  @Test public void stringIteratorOmitEmtpyStrings() {
    final String str = "&a=b&c=\"d&e=\"f\"&d=&=e&&";
    final String expected = "a=b|c=\"d|e=\"f\"|d=|=e";
    
    Iterator<String> i = new StringIterator(str, "&", true);
    StringBuilder actual = new StringBuilder();
    if (i.hasNext()) {
      actual.append(i.next());
      while(i.hasNext()) {
        actual.append('|').append(i.next());
      }
    }
    assertEquals(expected, actual.toString());
  }

  @Test public void stringIteratorRemove() {
    Iterator<String> i = new StringIterator("a=b", "=");
    try {
      i.remove();
    } catch (UnsupportedOperationException ex) {
      return;
    }
    fail();
  }
  
  @Test public void stringIteratorHasNext() {
    Iterator<String> i = new StringIterator("a=b", "=");
    while (i.hasNext()) {
      i.next();
    }
    assertEquals(false, i.hasNext());
    try {
      i.next();
    } catch (NoSuchElementException ex) {
      return;
    }
    fail();
  }
}
