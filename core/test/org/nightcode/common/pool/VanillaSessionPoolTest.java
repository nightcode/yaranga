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

import org.nightcode.common.pool.metadata.NamedEndpoint;
import org.nightcode.common.util.ExecutorUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link VanillaSessionPoolTest}.
 */
public class VanillaSessionPoolTest {

  @Test public void testInterrupt() {
    try (SessionPool<String, Session<String>> pool = SessionPoolBuilder.<String, Session<String>>instance("test")
        .addEndpoint(new NamedEndpoint("testEndpoint"))
        .sessionFactory(context -> new AbstractSession<>(context.endpoint()) {
          @Override protected void createImpl() {
            ExecutorUtils.sleepUninterruptibly(5, TimeUnit.SECONDS);
          }

          @Override protected void destroyImpl() {
            // do nothing
          }
        })
        .build()) {
      new Thread(() -> {
        ExecutorUtils.sleepUninterruptibly(1, TimeUnit.SECONDS);
        pool.close();
      }).start();
      pool.init().get(10, TimeUnit.SECONDS);
      Assert.fail("should throw ExecutionException");
    } catch (Exception ex) {
      Assert.assertEquals("java.lang.IllegalStateException: pool has been shut down", ex.getMessage());
    }
  }
}
