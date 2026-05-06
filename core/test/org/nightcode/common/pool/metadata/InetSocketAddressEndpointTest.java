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

package org.nightcode.common.pool.metadata;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link InetSocketAddressEndpoint}.
 */
public class InetSocketAddressEndpointTest  {

  @Test public void testInstantiate() {
    Endpoint<InetSocketAddress> endpoint = new InetSocketAddressEndpoint("127.0.0.1:443");
    InetSocketAddress address = endpoint.resolve();
    assertEquals("127.0.0.1", address.getHostString());
    assertEquals(443, address.getPort());
    assertNotNull(endpoint.id());

    endpoint = new InetSocketAddressEndpoint(address);
    address = endpoint.resolve();
    assertEquals("127.0.0.1", address.getHostString());
    assertEquals(443, address.getPort());
    assertNotNull(endpoint.id());
  }

  @Test public void testId() {
    Endpoint<InetSocketAddress> endpoint1 = new InetSocketAddressEndpoint("127.0.0.1:443");
    InetSocketAddress address1 = endpoint1.resolve();

    Endpoint<InetSocketAddress> endpoint2 = new InetSocketAddressEndpoint("127.0.0.1:443");
    InetSocketAddress address2 = endpoint2.resolve();
    
    assertNotEquals(endpoint1, endpoint2);
    assertEquals(address1, address2);
  }

  @Test public void testEquals() {
    Endpoint<InetSocketAddress> endpoint1 = new InetSocketAddressEndpoint("127.0.0.1:443");
    InetSocketAddress address1 = endpoint1.resolve();

    Endpoint<InetSocketAddress> endpoint2 = new InetSocketAddressEndpoint("127.0.0.1:443", endpoint1.id());
    InetSocketAddress address2 = endpoint2.resolve();

    assertEquals(endpoint1, endpoint2);
    assertEquals(address1, address2);


    endpoint1 = new InetSocketAddressEndpoint(address1);
    address1 = endpoint1.resolve();

    endpoint2 = new InetSocketAddressEndpoint(address1, endpoint1.id());
    address2 = endpoint2.resolve();

    assertEquals(endpoint1, endpoint2);
    assertEquals(address1, address2);
  }

  @Test public void testToString() {
    Endpoint<InetSocketAddress> endpoint = new InetSocketAddressEndpoint("127.0.0.1:443");
    String id = endpoint.id();
    assertEquals("/127.0.0.1:443-" + id, endpoint.toString());
  }
}
