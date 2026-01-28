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

package org.nightcode.net;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.nightcode.common.pool.Session;

/**
 * Base pipe interface.
 *
 * @param <A> the pipe address
 * @param <Q> the request packet
 * @param <R> the response packet
 */
public interface Pipe<A, Q, R> extends Session<A> {

  IOException EOF = new EOFException("closed");

  CompletableFuture<Void> sendAsync(Q packet);

  CompletableFuture<R> sendReceiveAsync(Q packet);
}
