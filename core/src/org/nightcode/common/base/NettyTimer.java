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

import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.nightcode.common.lang.Timer;
import org.nightcode.common.lang.TimerTask;

import static org.nightcode.common.util.ExecutorUtils.namedThreadFactory;

/**
 * Netty based Timer implementation.
 */
public class NettyTimer implements Timer {

  private final io.netty.util.Timer timer;

  public NettyTimer(String name) {
    this.timer = new HashedWheelTimer(namedThreadFactory(name + "-timer"));
  }

  @Override public void close() {
    timer.stop();
  }

  @Override public TimerTask schedule(Runnable runnable, long delay, TimeUnit unit) {
    Timeout timeout = timer.newTimeout(t -> runnable.run(), delay, unit);

    return new TimerTask() {
      @Override public boolean cancel() {
        return timeout.cancel();
      }

      @Override public boolean isCancelled() {
        return timeout.isCancelled();
      }

      @Override public boolean isExpired() {
        return timeout.isExpired();
      }

      @Override public Timer timer() {
        return NettyTimer.this;
      }
    };
  }
}
