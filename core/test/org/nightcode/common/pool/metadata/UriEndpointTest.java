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

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link UriEndpoint}.
 */
public class UriEndpointTest {

  @Test public void testInstantiate() {
    Endpoint<URI> endpoint = new UriEndpoint("https://example.com/api");
    URI           address  = endpoint.resolve();
    Assert.assertEquals("https", address.getScheme());
    Assert.assertEquals("example.com", address.getHost());
    Assert.assertEquals(-1, address.getPort());
    Assert.assertEquals("/api", address.getPath());
    Assert.assertNotNull(endpoint.id());

    endpoint = new UriEndpoint(address);
    address  = endpoint.resolve();
    Assert.assertEquals("https", address.getScheme());
    Assert.assertEquals("example.com", address.getHost());
    Assert.assertEquals(-1, address.getPort());
    Assert.assertEquals("/api", address.getPath());
    Assert.assertNotNull(endpoint.id());
  }

  @Test public void testId() {
    Endpoint<URI> endpoint1 = new UriEndpoint("https://example.com/api");
    URI           address1  = endpoint1.resolve();

    Endpoint<URI> endpoint2 = new UriEndpoint("https://example.com/api");
    URI           address2  = endpoint2.resolve();

    Assert.assertNotEquals(endpoint1, endpoint2);
    Assert.assertEquals(address1, address2);
  }

  @Test public void testEquals() {
    Endpoint<URI> endpoint1 = new UriEndpoint("https://example.com/api");
    URI           address1  = endpoint1.resolve();

    Endpoint<URI> endpoint2 = new UriEndpoint("https://example.com/api", endpoint1.id());
    URI           address2  = endpoint2.resolve();

    Assert.assertEquals(endpoint1, endpoint2);
    Assert.assertEquals(address1, address2);


    endpoint1 = new UriEndpoint(address1);
    address1  = endpoint1.resolve();

    endpoint2 = new UriEndpoint(address1, endpoint1.id());
    address2  = endpoint2.resolve();

    Assert.assertEquals(endpoint1, endpoint2);
    Assert.assertEquals(address1, address2);
  }

  @Test public void testToString() {
    Endpoint<URI> endpoint = new UriEndpoint("https://example.com/api");
    String        id       = endpoint.id();
    Assert.assertEquals("https://example.com/api-" + id, endpoint.toString());
  }
}
