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

import com.google.protobuf.Message;

import java.util.concurrent.CompletableFuture;

import org.nightcode.api.message.Metadata;

/**
 * API gateway call interface.
 *
 * @param <Q> the request
 * @param <R> the response
 */
public interface ApiGwCall<Q extends Message, R extends Message> {

  CompletableFuture<R> executeAsync(String serviceName, MethodHandler<Q, R> methodHandler, Q message, Metadata metadata);
}
