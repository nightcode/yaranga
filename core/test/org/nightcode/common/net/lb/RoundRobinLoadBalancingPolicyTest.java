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
import org.nightcode.common.net.Connection.ConnectionEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RoundRobinLoadBalancingPolicyTest {

  private static InetSocketAddress ADDRESS = InetSocketAddress.createUnresolved("localhost", 12345);

  private final Connection<InetSocketAddress> connection = new Connection<InetSocketAddress>("connection", ADDRESS) {
    @Override public void close() {
      // do nothing
    }

    @Override public void open() {
      // do nothing
    }
  };

  @Test public void testOpen() {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();

    Iterator<Connection<InetSocketAddress>> iterator = lbPolicy.selectConnections();
    Assert.assertFalse(iterator.hasNext());

    lbPolicy.onEvent(new ConnectionEvent<>(connection, Connection.State.ACTIVE));
    iterator = lbPolicy.selectConnections();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(connection, iterator.next());
  }

  @Test public void testClose() {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();
    lbPolicy.onEvent(new ConnectionEvent<>(connection, Connection.State.ACTIVE));
    Iterator<Connection<InetSocketAddress>> iterator = lbPolicy.selectConnections();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(connection, iterator.next());

    lbPolicy.onEvent(new ConnectionEvent<>(connection, Connection.State.CLOSED));
    iterator = lbPolicy.selectConnections();
    Assert.assertFalse(iterator.hasNext());
  }

  @Test public void testConnectionIterator() throws IOException {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();

    Connection<InetSocketAddress> connection1 = new Connection<InetSocketAddress>("connection1", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        fireStateEvent(State.ACTIVE);
      }
    };
    Connection<InetSocketAddress> connection2 = new Connection<InetSocketAddress>("connection2", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        fireStateEvent(State.ACTIVE);
      }
    };

    lbPolicy.addConnection(connection1);
    lbPolicy.addConnection(connection2);

    Iterator<Connection<InetSocketAddress>> iterator = lbPolicy.selectConnections();
    Assert.assertFalse(iterator.hasNext());

    connection1.open();
    connection2.open();

    iterator = lbPolicy.selectConnections();

    Connection<InetSocketAddress> target1 = iterator.next();
    Connection<InetSocketAddress> target2 = iterator.next();

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

  @Test public void testSingleConnectionIterator() throws IOException {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();

    Connection<InetSocketAddress> connection = new Connection<InetSocketAddress>("connection", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        fireStateEvent(State.ACTIVE);
      }
    };

    lbPolicy.addConnection(connection);

    Iterator<Connection<InetSocketAddress>> connections = lbPolicy.selectConnections();
    Assert.assertFalse(connections.hasNext());

    connection.open();

    connections = lbPolicy.selectConnections();

    Assert.assertTrue(connections.hasNext());
    Connection target = connections.next();
    Assert.assertEquals(connection, target);
    Assert.assertFalse(connections.hasNext());

    connections = lbPolicy.selectConnections();

    Assert.assertTrue(connections.hasNext());
    target = connections.next();
    Assert.assertEquals(connection, target);
    Assert.assertFalse(connections.hasNext());

    try {
      connections.next();
      Assert.fail("must throw NoSuchElementException");
    } catch (NoSuchElementException ex) {
      // do nothing
    }
  }

  @Test public void testAddConnection() throws IOException {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();

    Connection<InetSocketAddress> connection = new Connection<InetSocketAddress>("connection", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        fireStateEvent(State.ACTIVE);
      }
    };

    Iterator<Connection<InetSocketAddress>> connections = lbPolicy.selectConnections();
    Assert.assertFalse(connections.hasNext());

    lbPolicy.addConnection(connection);
    connection.open();

    connections = lbPolicy.selectConnections();
    Assert.assertTrue(connections.hasNext());
  }

  @Test public void testInit() {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();

    Connection<InetSocketAddress> connection = EasyMock.mock(Connection.class);

    EasyMock.expect(connection.addEventListener(lbPolicy)).andReturn(true).once();
    EasyMock.replay(connection);

    lbPolicy.addConnections(Collections.singletonList(connection));

    EasyMock.verify(connection);
  }

  @Test public void testRemoveConnection() throws IOException {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = new RoundRobinLoadBalancingPolicy<>();

    Connection<InetSocketAddress> connection = new Connection<InetSocketAddress>("connection", ADDRESS) {
      @Override public void close() {
        // do nothing
      }

      @Override public void open() {
        fireStateEvent(State.ACTIVE);
      }
    };

    lbPolicy.addConnection(connection);
    connection.open();

    Iterator<Connection<InetSocketAddress>> connections = lbPolicy.selectConnections();
    Assert.assertTrue(connections.hasNext());

    lbPolicy.removeConnection(connection);
    connections = lbPolicy.selectConnections();
    Assert.assertFalse(connections.hasNext());
  }

  @Test public void testDefaultLbPolicy() {
    LoadBalancingPolicy<InetSocketAddress, Connection<InetSocketAddress>> lbPolicy = LoadBalancingPolicy.defaultLoadBalancingPolicy();
    Assert.assertEquals(RoundRobinLoadBalancingPolicy.class, lbPolicy.getClass());
  }
}
