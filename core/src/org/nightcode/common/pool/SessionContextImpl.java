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
import org.nightcode.common.pool.metadata.Endpoint;

/**
 * Base implementation of the SessionContext.
 *
 * @param <A> the session address
 * @param <S> the session
 */
@Beta
public class SessionContextImpl<A, S extends Session<A>> implements SessionContext<A, S> {

  private final String            sessionName;
  private final Endpoint<A>       endpoint;
  private final SessionPool<A, S> pool;

  public SessionContextImpl(String sessionName, Endpoint<A> endpoint, SessionPool<A, S> pool) {
    this.sessionName = sessionName;
    this.endpoint    = endpoint;
    this.pool        = pool;
  }

  @Override public long createTimeoutMs() {
    return pool.createTimeoutMs();
  }

  @Override public void destroy(S session) {
    pool.operations().destroy(session);
  }

  @Override public Endpoint<A> endpoint() {
    return endpoint;
  }

  @Override public long executeTimeoutNs() {
    return pool.executeTimeoutNs();
  }

  @Override public void initialize(S session) {
    pool.operations().initialize(session);
  }

  @Override public SessionPool<A, S> pool() {
    return pool;
  }

  @Override public String poolName() {
    return pool.poolName();
  }

  @Override public long queueTimeoutNs() {
    return pool.queueTimeoutNs();
  }

  @Override public long rebuildTimeoutMs() {
    return pool.rebuildTimeoutMs();
  }

  @Override public S session() {
    return pool.factory().create(this);
  }

  @Override public String sessionName() {
    return sessionName;
  }
}
