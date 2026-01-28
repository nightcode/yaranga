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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.nightcode.common.base.NettyTimer;
import org.nightcode.common.lang.Timer;
import org.nightcode.common.pool.lb.LoadBalancingPolicy;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.util.Clock;

/**
 * todo.
 *
 * @param <A> the session address
 * @param <S> the session
 */
public final class SessionPoolBuilder<A, S extends Session<A>> {

  public static <A, S extends Session<A>> SessionPoolBuilder<A, S> instance(String name) {
    return new SessionPoolBuilder<>(name);
  }

  final String name;

  SessionFactory<A, S> factory;
  Timer                timer;

  SessionPoolOperations<A, S> operations          = SessionPoolOperations.def();
  LoadBalancingPolicy loadBalancingPolicy = LoadBalancingPolicy.def();
  Clock               clock               = Clock.sys();

  long createTimeoutMs  = 1_000L; // 1 sec.
  long rebuildTimeoutMs = 1_000L; // 1 sec.

  final List<Endpoint<A>> endpoints = new ArrayList<>();

  private SessionPoolBuilder(String name) {
    Objects.requireNonNull(name, "name");
    this.name = name;
  }

  public SessionPoolBuilder<A, S> addEndpoint(Endpoint<A> val) {
    Objects.requireNonNull(val, "endpoint");
    endpoints.add(val);
    return this;
  }

  public SessionPoolBuilder<A, S> addEndpoints(Collection<Endpoint<A>> val) {
    Objects.requireNonNull(val, "endpoints");
    endpoints.addAll(val);
    return this;
  }

  public SessionPool<A, S> build() {
    if (timer == null) {
      timer = new NettyTimer(name);
    }
    return new VanillaSessionPool<>(this);
  }

  public SessionPoolBuilder<A, S> clock(Clock val) {
    Objects.requireNonNull(val, "Clock");
    clock = val;
    return this;
  }

  public SessionPoolBuilder<A, S> createTimeout(long val, TimeUnit unit) {
    createTimeoutMs = unit.toMillis(val);
    return this;
  }

  public SessionPoolBuilder<A, S> loadBalancingPolicy(LoadBalancingPolicy val) {
    Objects.requireNonNull(val, "load balancing policy");
    loadBalancingPolicy = val;
    return this;
  }

  public SessionPoolBuilder<A, S> rebuildTimeout(long val, TimeUnit unit) {
    rebuildTimeoutMs = unit.toMillis(val);
    return this;
  }

  public SessionPoolBuilder<A, S> sessionFactory(SessionFactory<A, S> val) {
    Objects.requireNonNull(val, "session factory");
    factory = val;
    return this;
  }

  public SessionPoolBuilder<A, S> sessionPoolOperations(SessionPoolOperations<A, S> val) {
    Objects.requireNonNull(val, "session pool operations");
    operations = val;
    return this;
  }

  public SessionPoolBuilder<A, S> timer(Timer val) {
    Objects.requireNonNull(val, "Timer");
    timer = val;
    return this;
  }
}
