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

package org.nightcode.common.pool;

import java.util.concurrent.TimeUnit;

import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.pool.metadata.NamedEndpoint;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link SessionContext}.
 */
public class SessionContextTest {

  @Test public void context() {
    SessionPool<String, Session<String>> pool = SessionPoolBuilder.<String, Session<String>>instance("testPool")
        .createTimeout(10, TimeUnit.SECONDS)
        .executeTimeout(15, TimeUnit.SECONDS)
        .queueTimeout(20, TimeUnit.SECONDS)
        .rebuildTimeout(25, TimeUnit.SECONDS)
        .build();

    Endpoint<String>                        endpoint = new NamedEndpoint("testEndpoint");
    SessionContext<String, Session<String>> context  = new SessionContextImpl<>("testSession", endpoint, pool);

    Assert.assertEquals(TimeUnit.SECONDS.toMillis(10), context.createTimeoutMs());
    Assert.assertEquals(TimeUnit.SECONDS.toNanos(15), context.executeTimeoutNs());
    Assert.assertEquals(TimeUnit.SECONDS.toNanos(20), context.queueTimeoutNs());
    Assert.assertEquals(TimeUnit.SECONDS.toMillis(25), context.rebuildTimeoutMs());
    Assert.assertEquals(endpoint, context.endpoint());
    Assert.assertEquals(pool, context.pool());
    Assert.assertEquals("testPool", context.poolName());
    Assert.assertEquals("testSession", context.sessionName());
  }
}
