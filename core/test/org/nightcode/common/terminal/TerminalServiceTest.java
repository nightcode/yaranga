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

package org.nightcode.common.terminal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.nightcode.common.props.Properties;
import org.nightcode.common.props.SystemPropertiesStorage;
import org.nightcode.common.service.Service;
import org.nightcode.common.util.ExecutorUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link TerminalService}.
 */
public class TerminalServiceTest {

  interface ApiCommands {
    @Cmd(name = "echo", help = "echo command")
    String echo(@Arg(name = "src", alias = "s", help = "Echo message.", required = true) String src);
  }

  static final class ApiCommandsImpl implements ApiCommands {
    @Override public String echo(String src) {
      Terminal.get().printf(src);
      return src;
    }
  }

  @Test public void execute() throws ExecutionException, InterruptedException, TimeoutException {
    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty("org.nightcode.terminal.SIGTERM", "false");
    AtomicBoolean executed = new AtomicBoolean(false);
    AtomicBoolean read     = new AtomicBoolean(false);
    Terminal terminal = new Terminal() {
      @Override public void printf(String format, Object... args) {
        if (read.compareAndSet(false, true)) {
          Assert.assertEquals("test", format);
        }
      }

      @Override public String readLine(String format, Object... args) {
        if (executed.compareAndSet(false, true)) {
          return "echo -s test";
        }
        return "quit";
      }
    };
    TerminalService service = new TerminalService(terminal, new ApiCommandsImpl());
    service.startAsync().get(1, TimeUnit.SECONDS);
    ExecutorUtils.sleepUninterruptibly(1, TimeUnit.SECONDS);
    Assert.assertEquals(Service.State.TERMINATED, service.state());
  }

  @Test public void help() throws ExecutionException, InterruptedException, TimeoutException {
    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty("org.nightcode.terminal.SIGTERM", "false");
    AtomicBoolean executed = new AtomicBoolean(false);
    AtomicBoolean read     = new AtomicBoolean(false);
    Terminal terminal = new Terminal() {
      @Override public void printf(String format, Object... args) {
        if (read.compareAndSet(false, true)) {
          String expected = """
                               echo - echo command
                                   -s, --src            Echo message.""";
          Assert.assertEquals(expected, args.length > 0 ? args[0] : "");
        }
      }

      @Override public String readLine(String format, Object... args) {
        if (executed.compareAndSet(false, true)) {
          return "help echo";
        }
        return "quit";
      }
    };

    TerminalService service = new TerminalService(terminal, new ApiCommandsImpl());
    service.startAsync().get(1, TimeUnit.SECONDS);
    ExecutorUtils.sleepUninterruptibly(1, TimeUnit.SECONDS);
    Assert.assertEquals(Service.State.TERMINATED, service.state());
  }
}
