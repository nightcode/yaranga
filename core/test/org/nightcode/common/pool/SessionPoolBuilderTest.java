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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.nightcode.common.lang.Timer;
import org.nightcode.common.lang.TimerTask;
import org.nightcode.common.pool.lb.LoadBalancingPolicy;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.util.Clock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link SessionPoolBuilder}.
 */
public class SessionPoolBuilderTest {

  @Test public void builder() {
    Clock clock = new Clock() {
      @Override public long currentMillis() {
        return 0;
      }

      @Override public long nanoTime() {
        return 0;
      }
    };

    Timer timer = new Timer() {
      @Override public void close() {
        // do nothing
      }

      @Override public TimerTask schedule(Runnable runnable, long delay, TimeUnit unit) {
        return null;
      }
    };

    LoadBalancingPolicy loadBalancingPolicy = new LoadBalancingPolicy() {
      @Override public void onDeregister(Endpoint<?> endpoint) {

      }

      @Override public int healthyCount() {
        return 3952;
      }

      @Override public void onRegister(Endpoint<?> endpoint) {

      }

      @Override public <A> Iterator<Endpoint<A>> sessions() {
        return null;
      }
    };

    SessionPoolOperations<String, Session<String>> operations = new SessionPoolOperations<>() {
      @Override public void destroy(Session<String> session) {
        SessionPoolOperations.super.destroy(session);
      }
    };

    SessionFactory<String, Session<String>> factory = context -> null;

    SessionPool<String, Session<String>> pool = SessionPoolBuilder.<String, Session<String>>instance("testPool")
        .createTimeout(10, TimeUnit.SECONDS)
        .executeTimeout(15, TimeUnit.SECONDS)
        .queueTimeout(20, TimeUnit.SECONDS)
        .rebuildTimeout(25, TimeUnit.SECONDS)
        .sessionPoolOperations(operations)
        .loadBalancingPolicy(loadBalancingPolicy)
        .sessionFactory(factory)
        .clock(clock)
        .timer(timer)
        .build();

    assertEquals(TimeUnit.SECONDS.toMillis(10), pool.createTimeoutMs());
    assertEquals(TimeUnit.SECONDS.toNanos(15), pool.executeTimeoutNs());
    assertEquals(TimeUnit.SECONDS.toNanos(20), pool.queueTimeoutNs());
    assertEquals(TimeUnit.SECONDS.toMillis(25), pool.rebuildTimeoutMs());
    assertEquals(clock, pool.clock());
    assertEquals(timer, pool.timer());
    assertEquals(operations, pool.operations());
    assertEquals(factory, pool.factory());
    assertEquals(loadBalancingPolicy.healthyCount(), pool.sessionsHealthy());
  }
}
