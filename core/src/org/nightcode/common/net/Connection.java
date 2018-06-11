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

package org.nightcode.common.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public abstract class Connection implements Closeable {

  /**
   * Connection's state listener.
   */
  public interface StateListener {

    void onOpen(Connection connection);

    void onClose(Connection connection);
  }

  private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<>();

  @Override public void close() throws IOException {
    for (StateListener listener : stateListeners) {
      listener.onClose(this);
    }
  }

  public void open() throws IOException {
    for (StateListener listener : stateListeners) {
      listener.onOpen(this);
    }
  }

  public abstract <Q, R> CompletableFuture<R> executeAsync(Q request);

  public boolean addStateListener(StateListener listener) {
    return stateListeners.add(listener);
  }

  public boolean removeStateListener(StateListener listener) {
    return stateListeners.remove(listener);
  }
}
