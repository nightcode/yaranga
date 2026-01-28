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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.nightcode.common.util.Throwables;

import static java.lang.String.format;

/**
 * Implementation of the Endpoint for the InetSocketAddress.
 */
public class InetSocketAddressEndpoint implements Endpoint<InetSocketAddress> {

  private static final ConcurrentMap<InetSocketAddress, AtomicInteger> ID_GENERATOR = new ConcurrentHashMap<>();

  private static void checkPort(int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("illegal port value: " + port);
    }
  }

  private static AtomicInteger getIdGenerator(InetSocketAddress address) {
    return ID_GENERATOR.computeIfAbsent(address, h -> new AtomicInteger(-1));
  }

  private static InetSocketAddress parseAddress(String address) {
    Objects.requireNonNull(address, "address");

    String host;
    int    port;
    if (address.charAt(0) == '[') {
      int colonIndex        = address.indexOf(':');
      int closeBracketIndex = address.lastIndexOf(']');
      if (colonIndex < 0 || closeBracketIndex < colonIndex) {
        throw new IllegalArgumentException("illegal address: " + address);
      }
      host = address.substring(1, closeBracketIndex);
      if (closeBracketIndex + 1 == address.length() || address.charAt(closeBracketIndex + 1) != ':') {
        throw new IllegalArgumentException("illegal port value in address: " + address);
      }
      port = Integer.parseInt(address.substring(closeBracketIndex + 2));
    } else {
      int colonIndex = address.indexOf(':');
      if (colonIndex < 0 || address.indexOf(':', colonIndex + 1) >= 0) {
        throw new IllegalArgumentException("illegal address: " + address);
      }
      host = address.substring(0, colonIndex);
      port = Integer.parseInt(address.substring(colonIndex + 1));
    }

    InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByName(host);
    } catch (UnknownHostException ex) {
      throw Throwables.rethrow(ex);
    }
    checkPort(port);

    return new InetSocketAddress(inetAddress, port);
  }

  private final String            id;
  private final InetSocketAddress address;

  public InetSocketAddressEndpoint(String address) {
    this.address = parseAddress(address);
    this.id      = format("%04d", getIdGenerator(this.address).incrementAndGet());
  }

  public InetSocketAddressEndpoint(InetSocketAddress address) {
    Objects.requireNonNull(address, "address");
    this.address = address;
    this.id      = format("%04d", getIdGenerator(address).incrementAndGet());
  }

  public InetSocketAddressEndpoint(String address, String id) {
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(id, "id");
    this.address = parseAddress(address);
    this.id      = id;
  }

  public InetSocketAddressEndpoint(InetSocketAddress address, String id) {
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(id, "id");
    this.address = address;
    this.id      = id;
  }

  @Override public String id() {
    return id;
  }

  @Override public InetSocketAddress resolve() {
    return address;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof InetSocketAddressEndpoint that)) {
      return false;
    }
    return id.equals(that.id)
        && address.equals(that.address);
  }

  @Override public int hashCode() {
    int result = 17;
    result = result * 31 + id.hashCode();
    result = result * 31 + address.hashCode();
    return result;
  }

  @Override public String toString() {
    return address.toString() + '-' + id;
  }
}
