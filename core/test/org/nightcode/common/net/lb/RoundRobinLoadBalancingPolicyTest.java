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

package org.nightcode.common.net.lb;

import org.nightcode.common.net.Connection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RoundRobinLoadBalancingPolicyTest {

  private final Connection connection = new Connection() {
    @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
      throw new IllegalStateException();
    }
  };

  @Test public void testInit() {
    LoadBalancingPolicy lbPolicy = new RoundRobinLoadBalancingPolicy();

    Connection connection = EasyMock.mock(Connection.class);

    EasyMock.expect(connection.addStateListener(lbPolicy)).andReturn(true).once();
    EasyMock.replay(connection);

    lbPolicy.init(Collections.singletonList(connection));

    EasyMock.verify(connection);
  }

  @Test public void testOnOpen() {
    LoadBalancingPolicy lbPolicy = new RoundRobinLoadBalancingPolicy();

    Iterator<Connection> iterator = lbPolicy.selectConnections();
    Assert.assertFalse(iterator.hasNext());

    lbPolicy.onOpen(connection);
    iterator = lbPolicy.selectConnections();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(connection, iterator.next());
  }

  @Test public void testOnClose() {
    LoadBalancingPolicy lbPolicy = new RoundRobinLoadBalancingPolicy();
    lbPolicy.onOpen(connection);
    Iterator<Connection> iterator = lbPolicy.selectConnections();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(connection, iterator.next());

    lbPolicy.onClose(connection);
    iterator = lbPolicy.selectConnections();
    Assert.assertFalse(iterator.hasNext());
  }

  @Test public void testConnectionIterator() throws IOException {
    LoadBalancingPolicy lbPolicy = new RoundRobinLoadBalancingPolicy();

    Connection connection1 = new Connection() {
      @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
        throw new IllegalStateException();
      }
    };
    Connection connection2 = new Connection() {
      @Override public <Q, R> CompletableFuture<R> executeAsync(Q request) {
        throw new IllegalStateException();
      }
    };

    lbPolicy.init(Arrays.asList(connection1, connection2));

    Iterator<Connection> iterator = lbPolicy.selectConnections();

    Assert.assertFalse(iterator.hasNext());


    connection1.open();
    connection2.open();

    iterator = lbPolicy.selectConnections();

    Connection target1 = iterator.next();
    Connection target2 = iterator.next();

    Assert.assertEquals(connection1, target2);
    Assert.assertEquals(connection2, target1);
    Assert.assertFalse(iterator.hasNext());


    iterator = lbPolicy.selectConnections();

    target1 = iterator.next();
    target2 = iterator.next();

    Assert.assertEquals(connection1, target1);
    Assert.assertEquals(connection2, target2);
    Assert.assertFalse(iterator.hasNext());
  }
}
