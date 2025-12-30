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

import java.util.function.Supplier;

import org.nightcode.common.util.Throwables;

public interface ThrowingSupplier<T, E extends Throwable> {

  static <T, E extends Throwable> Supplier<T> toSupplier(ThrowingSupplier<T, E> supplier) {
    return () -> {
      try {
        return supplier.get();
      } catch (Throwable ex) {
        throw Throwables.rethrow(ex);
      }
    };
  }

  T get() throws E;
}
