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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for {@link EmptyIterator}.   
 */
public class EmptyIteratorTest {
  
  @Test public void hasNext() {
    Iterator<?> emptyIterator = EmptyIterator.instance();
    assertFalse(emptyIterator.hasNext());
  }
  
  @Test public void next() {
    Iterator<?> emptyIterator = EmptyIterator.instance();
    try {
      emptyIterator.next();
    } catch (Exception ex) {
      assertInstanceOf(NoSuchElementException.class, ex);
      return;
    }
    fail();
  }
  
  @Test public void remove() {
    Iterator<?> emptyIterator = EmptyIterator.instance();
    try {
      emptyIterator.remove();
    } catch (Exception ex) {
      assertInstanceOf(UnsupportedOperationException.class, ex);
      return;
    }
    fail();
  }
}
