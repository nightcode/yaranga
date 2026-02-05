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

package org.nightcode.api;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.api.message.Status;
import org.nightcode.common.lang.Timer;
import org.nightcode.common.lang.TimerTask;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.retry.RetryPolicy;
import org.nightcode.common.util.Clock;
import org.nightcode.net.Pipe;

/**
 * API call handler.
 *
 * @param <A> the address
 */
public class ApiCallHandler<A> {

  private final class TimeoutTask implements Runnable {
    private final Request message;

    TimeoutTask(Request message) {
      this.message = message;
    }

    @Override public void run() {
      onTimeout(new IOException("[" + current + "] request timed out for message:\n\n" + message));
    }
  }

  private static final CompletableFuture<Void> NIL = CompletableFuture.completedFuture(null);

  private volatile Pipe<A, Request, Response> current;
  private volatile CompletableFuture<?>       messageFuture;
  private volatile TimerTask                  timeout;

  private volatile Throwable lastExceptionCause;

  private final Request     request;
  private final int         maxAttempts;
  private final RetryPolicy retryPolicy;

  private final Clock                clock;
  private final Timer                timer;
  private final long                 deadlineNs;
  private final Iterator<ApiPipe<A>> connections;

  private final AtomicInteger attempts = new AtomicInteger(0);

  public ApiCallHandler(Request request, RetryPolicy retryPolicy, ApiContext<A> context) {
    this(request, context.maxAttempts(), context.executeTimeoutMs(), retryPolicy, context.connectionPool());
  }

  public ApiCallHandler(Request request, int maxAttempts, long executeTimeoutMs, RetryPolicy retryPolicy,
                        SessionPool<A, ApiPipe<A>> connectionPool) {
    this.request     = request;
    this.maxAttempts = maxAttempts;
    this.retryPolicy = retryPolicy;

    this.clock       = connectionPool.clock();
    this.timer       = connectionPool.timer();
    this.deadlineNs  = TimeUnit.MILLISECONDS.toNanos(executeTimeoutMs) + clock.nanoTime();
    this.connections = connectionPool.getSessions();
  }

  public CompletableFuture<Void> sendAsync() {
    return send(conn -> conn.sendAsync(request), (r, t) -> {
      cancelTimeout();
      if (t != null) {
        lastExceptionCause = t;

        RetryPolicy.Decision decision = retryPolicy.onRequestError(t);
        return switch (decision) {
          case RETRY -> sendAsync();
          default -> CompletableFuture.failedFuture(t);
        };
      }

      return NIL;
    });
  }

  public CompletableFuture<Response> sendReceiveAsync() {
    return send(conn -> conn.sendReceiveAsync(request), (r, t) -> {
      cancelTimeout();
      if (r != null && r.hasContent()) {
        return CompletableFuture.completedFuture(r);
      }
      if (r != null && t == null) {
        t = responseException(r);
      }
      lastExceptionCause = t;

      RetryPolicy.Decision decision = retryPolicy.onRequestError(t);
      return switch (decision) {
        case RETRY -> sendReceiveAsync();
        default -> CompletableFuture.failedFuture(t);
      };
    });
  }

  private <T> CompletableFuture<T> send(Function<Pipe<A, Request, Response>, CompletableFuture<T>> sendFn,
                                        BiFunction<? super T, Throwable, ? extends CompletableFuture<T>> handler) {
    if (attempts.incrementAndGet() > maxAttempts) {
      return CompletableFuture.failedFuture(new CompletionException("too many retries", lastExceptionCause));
    }

    if (deadlineNs > 0) {
      long diff = deadlineNs - clock.nanoTime();
      timeout = timer.schedule(new TimeoutTask(request), diff, TimeUnit.NANOSECONDS);
    }
    Pipe<A, Request, Response> connection = null;
    if (connections.hasNext()) {
      connection = connections.next();
    }
    if (connection == null) {
      if (attempts.get() == 1) {
        return CompletableFuture.failedFuture(new IOException("no connection available"));
      } else {
        return CompletableFuture.failedFuture(lastExceptionCause);
      }
    }
    current = connection;

    CompletableFuture<T> messageFuture = sendFn.apply(connection);
    this.messageFuture = messageFuture;

    return messageFuture.handle(handler).thenCompose(UnaryOperator.identity());
  }

  private Exception responseException(Response response) {
    Status status = response.getError();
    return new ApiException(status.getCode(), status.getMessage());
  }

  private void cancelTimeout() {
    final TimerTask t = timeout;
    if (t != null) {
      t.cancel();
    }
  }

  private void onTimeout(Exception cause) {
    final CompletableFuture<?> cf = messageFuture;
    if (cf != null) {
      cf.completeExceptionally(cause);
    }
  }
}
