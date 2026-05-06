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

package org.nightcode.common.base;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.nightcode.common.lang.Timer;
import org.nightcode.common.lang.TimerTask;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NettyTimer}.
 */
public class NettyTimerTest {

  @Test public void schedule() throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    try (Timer timer = new NettyTimer("test")) {
      TimerTask task = timer.schedule(() -> future.complete(Boolean.TRUE), 1, TimeUnit.SECONDS);
      assertTrue(future.get(2, TimeUnit.SECONDS));
      assertFalse(task.isCancelled());
      assertTrue(task.isExpired());
      assertEquals(timer, task.timer());
    }
  }

  @Test public void scheduleCancel() {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    try (Timer timer = new NettyTimer("test")) {
      TimerTask task = timer.schedule(() -> future.complete(Boolean.TRUE), 5, TimeUnit.SECONDS);
      task.cancel();
      assertTrue(task.isCancelled());
      assertFalse(task.isExpired());
    }
  }
}
