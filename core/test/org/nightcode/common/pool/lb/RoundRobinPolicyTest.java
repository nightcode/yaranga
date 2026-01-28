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

package org.nightcode.common.pool.lb;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.pool.metadata.NamedEndpoint;

import org.junit.Assert;
import org.junit.Test;

public class RoundRobinPolicyTest {

  private static final Endpoint<String> ENDPOINT = new NamedEndpoint("");

  @Test public void testOpen() {
    LoadBalancingPolicy lbPolicy = new RoundRobinPolicy();

    Iterator<Endpoint<String>> iterator = lbPolicy.sessions();
    Assert.assertFalse(iterator.hasNext());

    lbPolicy.onRegister(ENDPOINT);
    iterator = lbPolicy.sessions();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(ENDPOINT, iterator.next());
  }

  @Test public void testClose() {
    LoadBalancingPolicy lbPolicy = new RoundRobinPolicy();

    lbPolicy.onRegister(ENDPOINT);
    Iterator<Endpoint<String>> iterator = lbPolicy.sessions();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(ENDPOINT, iterator.next());

    lbPolicy.onDeregister(ENDPOINT);
    iterator = lbPolicy.sessions();
    Assert.assertFalse(iterator.hasNext());
  }

  @Test public void testConnectionIterator() {
    LoadBalancingPolicy lbPolicy = new RoundRobinPolicy();

    Endpoint<String> endpoint1 = new NamedEndpoint("1");
    Endpoint<String> endpoint2 = new NamedEndpoint("2");

    Iterator<Endpoint<String>> iterator = lbPolicy.sessions();
    Assert.assertFalse(iterator.hasNext());

    lbPolicy.onRegister(endpoint1);
    lbPolicy.onRegister(endpoint2);

    iterator = lbPolicy.sessions();

    Endpoint<String> target1 = iterator.next();
    Endpoint<String> target2 = iterator.next();

    Assert.assertEquals(endpoint1, target2);
    Assert.assertEquals(endpoint2, target1);
    Assert.assertFalse(iterator.hasNext());

    iterator = lbPolicy.sessions();

    target1 = iterator.next();
    target2 = iterator.next();

    Assert.assertEquals(endpoint1, target1);
    Assert.assertEquals(endpoint2, target2);
    Assert.assertFalse(iterator.hasNext());
  }

  @Test public void testSingleConnectionIterator() {
    LoadBalancingPolicy lbPolicy = new RoundRobinPolicy();

    Iterator<Endpoint<String>> sessions = lbPolicy.sessions();
    Assert.assertFalse(sessions.hasNext());

    lbPolicy.onRegister(ENDPOINT);

    sessions = lbPolicy.sessions();

    Assert.assertTrue(sessions.hasNext());
    Endpoint<String> target = sessions.next();
    Assert.assertEquals(ENDPOINT, target);
    Assert.assertFalse(sessions.hasNext());

    sessions = lbPolicy.sessions();

    Assert.assertTrue(sessions.hasNext());
    target = sessions.next();
    Assert.assertEquals(ENDPOINT, target);
    Assert.assertFalse(sessions.hasNext());

    try {
      sessions.next();
      Assert.fail("must throw NoSuchElementException");
    } catch (NoSuchElementException ex) {
      // do nothing
    }
  }

  @Test public void testOnRegister() {
    LoadBalancingPolicy lbPolicy = new RoundRobinPolicy();

    Iterator<Endpoint<String>> sessions = lbPolicy.sessions();
    Assert.assertFalse(sessions.hasNext());

    lbPolicy.onRegister(ENDPOINT);

    sessions = lbPolicy.sessions();
    Assert.assertTrue(sessions.hasNext());
  }

  @Test public void testOnDeregister() {
    LoadBalancingPolicy lbPolicy = new RoundRobinPolicy();

    lbPolicy.onRegister(ENDPOINT);

    Iterator<Endpoint<String>> sessions = lbPolicy.sessions();
    Assert.assertTrue(sessions.hasNext());

    lbPolicy.onDeregister(ENDPOINT);
    sessions = lbPolicy.sessions();
    Assert.assertFalse(sessions.hasNext());
  }

  @Test public void testDefaultLbPolicy() {
    LoadBalancingPolicy lbPolicy = LoadBalancingPolicy.def();
    Assert.assertEquals(RoundRobinPolicy.class, lbPolicy.getClass());
  }
}
