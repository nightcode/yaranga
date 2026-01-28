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

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.lang.Timer;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.util.Clock;

/**
 * todo.
 *
 * @param <A> the session address
 * @param <S> the session
 */
@Beta
public interface SessionPool<A, S extends Session<A>> extends SessionPoolContext<A, S>, Session.StateListener<A>, Closeable {

  S addSession(Endpoint<A> endpoint);

  S addSession(Endpoint<A> endpoint, Session.StateListener<A>... listeners);

  Clock clock();

  SessionFactory<A, S> factory();

  S getSession(Endpoint<A> endpoint);

  Iterator<S> getSessions();

  CompletableFuture<Void> init();

  SessionPoolOperations<A, S> operations();

  S removeSession(Endpoint<A> endpoint);

  void shutdown();

  int sessionsTotal();

  int sessionsHealthy();

  Timer timer();

  @Override default void close() {
    shutdown();
  }
}
