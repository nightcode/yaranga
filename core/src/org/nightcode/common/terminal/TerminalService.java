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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;
import org.nightcode.common.service.AbstractExecutorService;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.common.util.ReflectUtils;
import org.nightcode.common.util.Throwables;

/**
 * Terminal service.
 */
public class TerminalService extends AbstractExecutorService {

  private static final Function<Method, Annotation> ANNOTATION_FUNCTION = ReflectUtils.annotationFunction(Cmd.class);

  private static final String[] EMPTY_ARRAY = new String[0];

  private final Terminal                    terminal;
  private final Map<String, CommandInvoker> invokers;
  private final String                      help;
  private final ExecutorService             cmdExecutor;

  public TerminalService(Terminal terminal, Object proxy) {
    this.terminal = terminal;

    StringBuilder sb = new StringBuilder();
    sb.append("command list:\n");

    Map<String, CommandInvoker> invokerMap = new TreeMap<>();
    Class<?>[]                  interfaces = proxy.getClass().getInterfaces();
    for (Class<?> cmdInterface : interfaces) {
      for (Method method : cmdInterface.getMethods()) {
        Cmd annotation = (Cmd) ANNOTATION_FUNCTION.apply(method);
        if (annotation != null) {
          CommandInvoker invoker = CommandInvoker.createInvoker(proxy, method);
          invokerMap.put(annotation.name(), invoker);
          Log.debug().log(getClass(), "invoker for command {} has been created", annotation.name());
        }
      }
    }
    invokers = Collections.unmodifiableMap(invokerMap);
    invokers.forEach((k, v) -> sb.append(getFirstLine(v.toString())).append('\n'));
    help = sb.toString();

    cmdExecutor = ExecutorUtils.singleThreadExecutor(getClass().getSimpleName());
  }

  @Override protected void startUp() {
    addEventListener(event -> {
      if (event.type() == State.RUNNING) {
        cmdExecutor.execute(() -> {
          Exception lastFailedCause = null;
          try {
            Terminal.set(terminal);
            while (state() == State.RUNNING) {
              try {
                if (lastFailedCause != null) {
                  Exception tmpException = lastFailedCause;
                  lastFailedCause = null;
                  Log.debug().log(getClass(), tmpException, "[{}]: terminal has been restarted", serviceName());
                }
                readCommand();
              } catch (Exception ex) {
                Log.warn().log(getClass(), ex, "[{}]: terminal exception", serviceName());
                lastFailedCause = ex;
              }
            }
          } catch (Throwable th) {
            Log.fatal().log(getClass(), th, "[{}]: terminal would be stopped. Unexpected error.", serviceName());
          } finally {
            Terminal.clear();
          }
          stopAsync();
        });
      }
    });
  }

  @Override protected void shutDown() {
    ExecutorUtils.shutdownGracefully(cmdExecutor, 1, TimeUnit.SECONDS);
    if (Properties.instance().getBoolean("org.nightcode.terminal.SIGTERM", true)) {
      executor().execute(() -> System.exit(0));
    }
  }

  private void readCommand() {
    final String line = terminal.readLine("\n%s", ">_$ ");
    String[]     args = CommandLineParser.parse(line).toArray(EMPTY_ARRAY);
    try {
      Iterator<String> iterator = Arrays.asList(args).iterator();
      while (iterator.hasNext()) {
        CommandInvoker invoker;
        String         command = iterator.next().toLowerCase();
        switch (command) {
          case "help" -> {
            if (iterator.hasNext()) {
              String helpArg = iterator.next();
              invoker = invokers.get(helpArg);
              if (invoker != null) {
                terminal.printf("\n%s", invoker.toString());
              } else {
                StringBuilder sb = new StringBuilder();
                List<CommandInvoker> commandInvokers = invokers.values().stream()
                    .filter(i -> i.getCommand() != null && i.getCommand().name().contains(helpArg)).toList();
                if (commandInvokers.isEmpty()) {
                  commandInvokers = invokers.values().stream()
                      .filter(i -> i.getCommand() != null && i.getCommand().help().contains(helpArg)).toList();
                }
                if (commandInvokers.size() == 1) {
                  commandInvokers.forEach(commandInvoker -> sb.append(commandInvoker.toString()).append('\n'));
                } else {
                  commandInvokers.forEach(commandInvoker -> sb.append(getFirstLine(commandInvoker.toString())).append('\n'));
                }
                if (sb.toString().isEmpty()) {
                  terminal.printf("\n  unsupported command '%s'\n", helpArg);
                  terminal.printf("\n%s", help);
                } else {
                  terminal.printf(sb.toString());
                }
              }
            } else {
              terminal.printf("\n%s", help);
            }
          }
          case "exit", "quit" -> {
            terminal.printf("Bye bye!\n");
            stopAsync();
          }
          default -> {
            invoker = invokers.get(command);
            if (invoker == null) {
              terminal.printf("\n  unsupported command '%s'\n", command);
              terminal.printf("\n%s", help);
            } else {
              invoker.invoke(iterator);
            }
          }
        }
      }
    } catch (CommandInvocationException ex) {
      Log.warn().log(getClass(), ex, "cannot execute command: {}", line);
      terminal.printf("cannot execute command. Reason: %s\n", Throwables.getRootCause(ex).getMessage());
    }
  }

  private String getFirstLine(String src) {
    if (src == null) {
      return src;
    }
    int ls = src.indexOf('\n');
    if (ls == -1) {
      return src;
    }
    return src.substring(0, ls);
  }
}
