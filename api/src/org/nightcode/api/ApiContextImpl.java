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

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import org.nightcode.api.message.Metadata;
import org.nightcode.common.pool.SessionPool;

/**
 * Default ApiContext implementation.
 *
 * @param <A> the address
 */
class ApiContextImpl<A> implements ApiContext<A> {

  private final String                     serviceName;
  private final MessagePackager            packager;
  private final SessionPool<A, ApiPipe<A>> connectionPool;
  private final Metadata                   metadata;
  private final long                       executeTimeoutMs;
  private final int                        maxAttempts;

  ApiContextImpl(String serviceName, MessagePackager packager, SessionPool<A, ApiPipe<A>> connectionPool,
                 Metadata metadata, long executeTimeoutMs, int maxAttempts) {
    this.serviceName      = serviceName;
    this.packager         = packager;
    this.connectionPool   = connectionPool;
    this.metadata         = metadata;
    this.executeTimeoutMs = executeTimeoutMs;
    this.maxAttempts      = maxAttempts;
  }

  @Override public SessionPool<A, ApiPipe<A>> connectionPool() {
    return connectionPool;
  }

  @Override public long executeTimeoutMs() {
    return executeTimeoutMs;
  }

  @Override public int maxAttempts() {
    return maxAttempts;
  }

  @Override public Metadata metadata() {
    return metadata;
  }

  @Override public <Q extends Message, R extends Message> ApiCall<Q, R> newApiCall(Class<Q> requestClass, Class<R> responseClass) {
    return new ApiCallImpl<>(this, responseClass);
  }

  @Override public <M extends Message> Any packMessage(M message) {
    return packager.pack(message);
  }

  @Override public String serviceName() {
    return serviceName;
  }
}
