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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.util.concurrent.CompletableFuture;

import org.nightcode.api.message.Metadata;
import org.nightcode.api.message.Request;
import org.nightcode.common.pool.retry.RetryPolicy;

/**
 * Default implementation of ApiCall.
 *
 * @param <A> the address
 * @param <Q> the request
 * @param <R> the response
 */
public class ApiCallImpl<A, Q extends Message, R extends Message> implements ApiCall<Q, R> {

  private final ApiContext<A> context;
  private final Class<R>      responseClass;

  public ApiCallImpl(ApiContext<A> context, Class<R> responseClass) {
    this.context       = context;
    this.responseClass = responseClass;
  }

  @Override public CompletableFuture<R> executeAsync(Q message, Metadata metadata) {
    Request request;
    try {
      Any payload = context.packMessage(message);
      request = Request.newBuilder()
          .setService(payload.getTypeUrl())
          .setMetadata(metadata)
          .setPayload(payload)
          .build();
    } catch (Exception ex) {
      return CompletableFuture.failedFuture(ex);
    }

    ApiCallHandler<A> handler = new ApiCallHandler<>(request, RetryPolicy.def(), context);
    return handler.sendReceiveAsync().thenCompose(r -> {
      if (!r.hasContent()) {
        return CompletableFuture.failedFuture(new ApiException(r.getError().getCode(), r.getError().getMessage()));
      }
      try {
        return CompletableFuture.completedFuture(r.getContent().unpack(responseClass));
      } catch (InvalidProtocolBufferException ex) {
        return CompletableFuture.failedFuture(ex);
      }
    });
  }
}
