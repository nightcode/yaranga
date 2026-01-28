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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Implementation of the Endpoint for the String.
 */
public class NamedEndpoint implements Endpoint<String> {

  private static final AtomicInteger ID = new AtomicInteger(0);

  private final String id;

  public NamedEndpoint() {
    id = format("%04d", ID.incrementAndGet());
  }

  public NamedEndpoint(String id) {
    Objects.requireNonNull(id, "session id");
    this.id = id;
  }

  @Override public String id() {
    return id;
  }

  @Override public String resolve() {
    return id;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof NamedEndpoint that)) {
      return false;
    }
    return id.equals(that.id);
  }

  @Override public int hashCode() {
    int result = 17;
    result = 31 * result + id.hashCode();
    return result;
  }

  @Override public String toString() {
    return id;
  }
}
