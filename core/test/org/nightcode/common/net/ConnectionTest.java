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

import org.nightcode.common.util.event.EventListener;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionTest {

  private static InetSocketAddress ADDRESS = InetSocketAddress.createUnresolved("localhost", 12345);

  @Test public void testAddStateListener() {
    Connection<InetSocketAddress> connection = new Connection<InetSocketAddress>("connection", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        // do nothing
      }

      @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
        return null;
      }
    };

    EventListener stateListener = EasyMock.createMock(EventListener.class);

    boolean target = connection.addEventListener(stateListener);
    Assert.assertTrue(target);

    target = connection.addEventListener(stateListener);
    Assert.assertFalse(target);
  }

  @Test public void testRemoveStateListener() {
    Connection<InetSocketAddress> connection = new Connection<InetSocketAddress>("connection", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        // do nothing
      }

      @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
        return null;
      }
    };

    EventListener stateListener = EasyMock.createMock(EventListener.class);
    
    boolean target = connection.removeEventListener(stateListener);
    Assert.assertFalse(target);

    connection.addEventListener(stateListener);

    target = connection.removeEventListener(stateListener);
    Assert.assertTrue(target);
  }
}
