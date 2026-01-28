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

import java.util.concurrent.CompletableFuture;

public class PacketFuture<V> extends CompletableFuture<V> {

  private final long                   packetId;
  private final PacketExceptionHandler exceptionHandler;

  public PacketFuture(long packetId, PacketExceptionHandler exceptionHandler) {
    this.packetId         = packetId;
    this.exceptionHandler = exceptionHandler;
  }

  @Override public boolean completeExceptionally(Throwable ex) {
    exceptionHandler.accept(packetId, ex);
    return super.completeExceptionally(ex);
  }
}
