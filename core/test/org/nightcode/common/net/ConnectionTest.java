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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionTest {

  @Test public void testAddStateListener() {
    Connection connection = new Connection() {
      @Override public void close() {
        // do nothing
      }

      @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
        return null;
      }
    };

    Connection.StateListener stateListener = EasyMock.createMock(Connection.StateListener.class);

    boolean target = connection.addStateListener(stateListener);
    Assert.assertTrue(target);

    target = connection.addStateListener(stateListener);
    Assert.assertFalse(target);
  }

  @Test public void testRemoveStateListener() {
    Connection connection = new Connection() {
      @Override public void close() {
        // do nothing
      }

      @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
        return null;
      }
    };

    Connection.StateListener stateListener = EasyMock.createMock(Connection.StateListener.class);
    
    boolean target = connection.removeStateListener(stateListener);
    Assert.assertFalse(target);

    connection.addStateListener(stateListener);

    target = connection.removeStateListener(stateListener);
    Assert.assertTrue(target);
  }
}
