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

package org.nightcode.common.pool;

import org.nightcode.common.annotations.Beta;

/**
 * Session pool operations.
 *
 * @param <A> the session address
 * @param <S> the session
 */
@Beta
public interface SessionPoolOperations<A, S extends Session<A>> {

  final class Def<A, S extends Session<A>> implements SessionPoolOperations<A, S> { }

  SessionPoolOperations<?, ?> DEF = new Def<>();

  static <A, S extends Session<A>> SessionPoolOperations<A, S> def() {
    // noinspection unchecked
    return (SessionPoolOperations<A, S>) DEF;
  }

  default void destroy(S session) {
    session.destroy();
  }

  default void initialize(S session) {
    session.initialize();
  }
}
