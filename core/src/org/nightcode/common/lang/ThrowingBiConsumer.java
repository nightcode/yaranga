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

import java.util.function.BiConsumer;

import org.nightcode.common.util.Throwables;

public interface ThrowingBiConsumer<T, U, E extends Throwable> {

  static <T, U, E extends Throwable> BiConsumer<T, U> toBiConsumer(ThrowingBiConsumer<T, U, E> biConsumer) {
    return (t, u) -> {
      try {
        biConsumer.accept(t, u);
      } catch (Throwable ex) {
        throw Throwables.rethrow(ex);
      }
    };
  }

  void accept(T t, U u) throws E;
}
