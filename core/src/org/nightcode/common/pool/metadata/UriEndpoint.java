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
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.nightcode.common.util.Throwables;

import static java.lang.String.format;

/**
 * Implementation of the Endpoint for the URI.
 */
public class UriEndpoint implements Endpoint<URI> {

  private static final ConcurrentMap<URI, AtomicInteger> ID_GENERATOR = new ConcurrentHashMap<>();

  private static AtomicInteger getIdGenerator(URI uri) {
    return ID_GENERATOR.computeIfAbsent(uri, h -> new AtomicInteger(-1));
  }

  private final String id;
  private final URI    uri;

  public UriEndpoint(String uri) {
    Objects.requireNonNull(uri, "uri");
    try {
      this.uri = new URI(uri);
    } catch (URISyntaxException ex) {
      throw Throwables.rethrow(ex);
    }
    this.id  = format("%04d", getIdGenerator(this.uri).incrementAndGet());
  }

  public UriEndpoint(URI uri) {
    Objects.requireNonNull(uri, "uri");
    this.uri = uri;
    this.id  = format("%04d", getIdGenerator(uri).incrementAndGet());
  }

  public UriEndpoint(String uri, String id) {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(id, "id");
    try {
      this.uri = new URI(uri);
    } catch (URISyntaxException ex) {
      throw Throwables.rethrow(ex);
    }
    this.id = id;
  }

  public UriEndpoint(URI uri, String id) {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(id, "id");
    this.uri = uri;
    this.id  = id;
  }

  @Override public String id() {
    return id;
  }

  @Override public URI resolve() {
    return uri;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof UriEndpoint that)) {
      return false;
    }
    return id.equals(that.id)
        && uri.equals(that.uri);
  }

  @Override public int hashCode() {
    int result = 17;
    result = result * 31 + id.hashCode();
    result = result * 31 + uri.hashCode();
    return result;
  }

  @Override public String toString() {
    return uri.toString() + '-' + id;
  }
}
