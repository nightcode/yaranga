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

package org.nightcode.common.base;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link Jvm}.
 */
public class JvmTest {

  @Test public void pid() {
    int pid = Jvm.pid();
    Assert.assertTrue(pid > 0);
  }

  @Test public void classPath() {
    String classPath = Jvm.classPath();
    Assert.assertEquals(System.getProperty("java.class.path"), classPath);
  }

  @Test public void libraryPath() {
    String libraryPath = Jvm.libraryPath();
    Assert.assertEquals(System.getProperty("java.library.path"), libraryPath);
  }

  @Test public void uptime() {
    String uptime = Jvm.uptime();
    Assert.assertNotNull(uptime);
  }

  @Test public void uptimeMs() {
    long uptime = Jvm.uptimeMs();
    Assert.assertTrue(uptime > 0);
  }

  @Test public void vmArguments() {
    String vmArguments = Jvm.vmArguments();
    Assert.assertNotNull(vmArguments);
  }
}
