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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.nightcode.common.base.AbstractIterator;
import org.nightcode.common.lang.Event;
import org.nightcode.common.lang.Timer;
import org.nightcode.common.pool.lb.LoadBalancingPolicy;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.util.Clock;
import org.nightcode.common.util.Closeables;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Vanilla SessionPool implementation.
 *
 * @param <A> the session address
 * @param <S> the session
 */
public class VanillaSessionPool<A, S extends Session<A>> implements SessionPool<A, S> {

  public enum State {
    NEW(0x01),
    RUNNING(0x02),
    SHUTDOWN(0x04),
    TERMINATED(0x08);

    private final int state;

    State(int state) {
      this.state = state;
    }

    public int state() {
      return state;
    }
  }

  private static final long QUEUE_TIMEOUT_MS   = 15_000;
  private static final long EXECUTE_TIMEOUT_MS = 10_000;

  private final String                      name;
  private final SessionFactory<A, S>        factory;
  private final SessionPoolOperations<A, S> operations;
  private final LoadBalancingPolicy         loadBalancingPolicy;
  private final Clock                       clock;
  private final Timer                       timer;
  private final List<Endpoint<A>>           endpoints;

  private final ConcurrentMap<Endpoint<A>, S>             sessions     = new ConcurrentHashMap<>();
  private final ConcurrentMap<Endpoint<A>, AtomicInteger> idGenerators = new ConcurrentHashMap<>();

  private final long createTimeoutMs;
  private final long rebuildTimeoutMs;
  private final long queueTimeoutNs;
  private final long executeTimeoutNs;

  private final CompletableFuture<Void> initFuture = new CompletableFuture<>();

  private final AtomicReference<State> control = new AtomicReference<>(State.NEW);

  private final ReentrantLock mainLock = new ReentrantLock();

  VanillaSessionPool(SessionPoolBuilder<A, S> builder) {
    name                = builder.name;
    factory             = builder.factory;
    operations          = builder.operations;
    loadBalancingPolicy = builder.loadBalancingPolicy;
    clock               = builder.clock;
    timer               = builder.timer;
    createTimeoutMs     = builder.createTimeoutMs;
    rebuildTimeoutMs    = builder.rebuildTimeoutMs;
    endpoints           = new ArrayList<>(builder.endpoints);

    long queueTimeoutMs   = Properties.instance().getLong(name + ".queueTimeoutMs", QUEUE_TIMEOUT_MS);
    long executeTimeoutMs = Properties.instance().getLong(name + ".executeTimeoutMs", EXECUTE_TIMEOUT_MS);

    queueTimeoutNs   = MILLISECONDS.toNanos(queueTimeoutMs);
    executeTimeoutNs = MILLISECONDS.toNanos(executeTimeoutMs);

    Log.info().log(getClass(), """
            SessionPool [{}] created with parameters:
            \tendpoints: {}
            \tcreateTimeoutMs: {}
            \trebuildTimeoutMs: {}
            \tqueueTimeoutMs: {}
            \texecuteTimeoutMs: {}"""
        , name, endpoints, createTimeoutMs, rebuildTimeoutMs, queueTimeoutMs, executeTimeoutMs);
  }

  @Override public S addSession(Endpoint<A> endpoint) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
      if (control.get() != State.RUNNING) {
        return null;
      }

      SessionContext<A, S> context = context(endpoint);

      S session = context.session();
      sessions.put(endpoint, session);

      return session;
    } finally {
      mainLock.unlock();
    }
  }

  @Override public S addSession(Endpoint<A> endpoint, Session.StateListener<A>... listeners) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
      S session = addSession(endpoint);
      if (session != null) {
        Stream.of(listeners).forEach(session::addListener);
      }
      return session;
    } finally {
      mainLock.unlock();
    }
  }

  @Override public Clock clock() {
    return clock;
  }

  @Override public long createTimeoutMs() {
    return createTimeoutMs;
  }

  @Override public long executeTimeoutNs() {
    return executeTimeoutNs;
  }

  @Override public SessionFactory<A, S> factory() {
    return factory;
  }

  @Override public S getSession(Endpoint<A> endpoint) {
    return sessions.get(endpoint);
  }

  @Override public Iterator<S> getSessions() {
    final Iterator<Endpoint<A>> i = loadBalancingPolicy.sessions();
    return new AbstractIterator<>() {
      @Override protected S computeNext() {
        if (!i.hasNext()) {
          return endOfData();
        }
        return getSession(i.next());
      }
    };
  }

  @Override public CompletableFuture<Void> init() {
    if (control.compareAndSet(State.NEW, State.RUNNING)) {
      final Session.StateListener<A> listener = event -> {
        if (Session.State.ACTIVE == event.type()) {
          initFuture.complete(null);
        }
      };

      for (Endpoint<A> endpoint : endpoints) {
        SessionContext<A, S> context = context(endpoint);

        S session = context.session();
        session.addListener(listener);

        sessions.put(endpoint, session);
      }

      initFuture.thenAccept(v -> {
        for (S session : sessions.values()) {
          session.removeListener(listener);
        }
      });

      if (endpoints.isEmpty()) {
        initFuture.complete(null);
      }
    }

    return initFuture;
  }

  @Override public void onEvent(Event<? extends Session<A>, Session.State> event) {
    Endpoint<A> endpoint = event.subject().endpoint();
    if (!sessions.containsKey(endpoint)) {
      return;
    }
    switch (event.type()) {
      case ACTIVE -> loadBalancingPolicy.onRegister(endpoint);
      case DESTROYING, DESTROYED -> loadBalancingPolicy.onDeregister(endpoint);
      default -> { }
    }
  }

  @Override public SessionPoolOperations<A, S> operations() {
    return operations;
  }

  @Override public SessionPool<A, S> pool() {
    return this;
  }

  @Override public String poolName() {
    return name;
  }

  @Override public long queueTimeoutNs() {
    return queueTimeoutNs;
  }

  @Override public long rebuildTimeoutMs() {
    return rebuildTimeoutMs;
  }

  @Override public S removeSession(Endpoint<A> endpoint) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
      S session = sessions.remove(endpoint);
      if (session == null) {
        throw new IllegalArgumentException("there is no session for endpoint: " + endpoint);
      }
      loadBalancingPolicy.onDeregister(endpoint);
      session.removeAllListeners();
      return session;
    } finally {
      mainLock.unlock();
    }
  }

  @Override public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
      control.set(State.SHUTDOWN);
      initFuture.completeExceptionally(new IllegalStateException("pool has been shut down"));
      for (S session : sessions.values()) {
        operations.destroy(session);
        session.removeListener(this);
      }
      Closeables.close(timer);
      control.set(State.TERMINATED);
    } finally {
      mainLock.unlock();
    }
  }

  @Override public int sessionsTotal() {
    return sessions.size();
  }

  @Override public int sessionsHealthy() {
    return loadBalancingPolicy.healthyCount();
  }

  @Override public Timer timer() {
    return timer;
  }

  private String buildSessionName(Endpoint<A> endpoint) {
    return poolName() + "_session_" + endpoint.toString() + '[' + getIdGenerator(endpoint).incrementAndGet() + ']';
  }

  private SessionContext<A, S> context(Endpoint<A> endpoint) {
    return new SessionContextImpl<>(buildSessionName(endpoint), endpoint, this);
  }

  private AtomicInteger getIdGenerator(Endpoint<A> endpoint) {
    return idGenerators.computeIfAbsent(endpoint, h -> new AtomicInteger(-1));
  }
}
