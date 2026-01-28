package org.nightcode.net.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.retry.RetryPolicy;
import org.nightcode.net.Transmitter;

public final class PacketHandler<A, P> {

  private volatile Throwable lastExceptionCause;

  private final P           packet;
  private final int         maxAttempts;
  private final RetryPolicy retryPolicy;

  private final Iterator<Transmitter<A, P>> transmitters;

  private final AtomicInteger attempts = new AtomicInteger(0);

  public PacketHandler(P packet, int maxAttempts, RetryPolicy retryPolicy, SessionPool<A, Transmitter<A, P>> pool) {
    this.packet      = packet;
    this.maxAttempts = maxAttempts;
    this.retryPolicy = retryPolicy;

    this.transmitters = pool.getSessions();
  }

  public CompletableFuture<Void> sendAsync() {
    CompletableFuture<Void> cf;
    if (attempts.incrementAndGet() > maxAttempts) {
      cf = CompletableFuture.failedFuture(new CompletionException("too many retries", lastExceptionCause));
    } else {
      Transmitter<A, P> transmitter = null;
      if (transmitters.hasNext()) {
        transmitter = transmitters.next();
      }
      if (transmitter == null) {
        if (attempts.get() == 1) {
          return CompletableFuture.failedFuture(new IOException("no transmitter available"));
        } else {
          return CompletableFuture.failedFuture(lastExceptionCause);
        }
      }

      cf = transmitter.send(packet).handle((r, t) -> {
        if (t != null) {
          lastExceptionCause = t;

          RetryPolicy.Decision decision = retryPolicy.onRequestError(t);
          return switch (decision) {
            case RETRY, TRY_NEXT -> sendAsync();
            default -> CompletableFuture.<Void>failedFuture(t);
          };
        }

        return CompletableFuture.<Void>completedFuture(null);
      }).thenCompose(UnaryOperator.identity());
    }

    return cf;
  }
}
