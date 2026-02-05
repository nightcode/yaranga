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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nightcode.common.util.Closeables;

/**
 * Default ApiGwContext implementation.
 */
public class ApiGwContextImpl implements ApiGwContext {

  private final int maxBodyLengthBytes;

  private final ConcurrentMap<String, ApiHandler> apiHandlers = new ConcurrentHashMap<>();

  public ApiGwContextImpl(int maxBodyLengthBytes) {
    this.maxBodyLengthBytes = maxBodyLengthBytes;
  }

  @Override public void close() {
    for (ApiHandler sh : apiHandlers.values()) {
      Closeables.close(sh);
    }
  }

  @Override public int maxBodyLengthBytes() {
    return maxBodyLengthBytes;
  }

  @Override public <Q extends Message, R extends Message> ApiGwCall<Q, R> newApiCall() {
    return new ApiGwCallImpl<>(this);
  }

  @Override public ConcurrentMap<String, ApiHandler> apiHandlers() {
    return apiHandlers;
  }
}
