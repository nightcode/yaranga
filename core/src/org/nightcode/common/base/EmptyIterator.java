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
 * An empty iterator.
 *
 * @param <E> The element type
 */
public final class EmptyIterator<E> extends ReadOnlyIterator<E> {

  private static final Iterator<?> INSTANCE = new EmptyIterator<>();

  private EmptyIterator() {
    // do nothing
  }

  /**
   * Returns false always.
   *
   * @return false
   */
  @Override public boolean hasNext() {
    return false;
  }

  /**
   * Guaranteed to throw an exception.
   *
   * @throws NoSuchElementException always
   */
  @Override public E next() {
    throw new NoSuchElementException();
  }

  /**
   * Returns INSTANCE of empty iterator.
   *
   * @param <E> an element's type
   * @return instance of empty iterator
   */
  @SuppressWarnings("unchecked")
  public static <E> Iterator<E> instance() {
    return (Iterator<E>) INSTANCE;
  }
}
