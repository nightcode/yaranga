package org.nightcode.net;

import java.util.concurrent.CompletableFuture;

import org.nightcode.common.pool.Session;

public interface Transmitter<A, M> extends Session<A> {

  CompletableFuture<Void> send(M message);
}
