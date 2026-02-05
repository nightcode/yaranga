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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.nightcode.common.logging.Log;
import org.nightcode.common.terminal.value.converter.BooleanValueConverter;
import org.nightcode.common.util.ReflectUtils;

/**
 * Command invoker.
 */
public class CommandInvoker {

  private static final Function<Method, Annotation> ANNOTATION_FUNCTION = ReflectUtils.annotationFunction(Cmd.class);

  static CommandInvoker createInvoker(Object proxy, Method method) {
    Cmd annotation = (Cmd) ANNOTATION_FUNCTION.apply(method);
    if (annotation == null) {
      throw new IllegalArgumentException("Method <" + method + "> should have @Cmd annotation.");
    }

    StringBuilder logMsg = new StringBuilder();
    if (Log.debug().isEnabled(CommandInvoker.class)) {
      logMsg.append("creating invoker for ")
          .append(method.getDeclaringClass().getSimpleName())
          .append('.').append(method.getName());
    }

    try {
      List<Argument> arguments = Argument.newInstances(method);
      if (Log.debug().isEnabled(CommandInvoker.class)) {
        logMsg.append("\n  arguments: ").append(arguments);
      }

      Command command = new Command(annotation.name(), annotation.help(), arguments, method);

      return new CommandInvoker(proxy, command);
    } finally {
      Log.debug().log(CommandInvoker.class, logMsg::toString);
    }
  }

  private final Command command;
  private final Object  proxy;
  private final String  toString;

  private final Map<String, Argument> map = new HashMap<>();

  CommandInvoker(Object proxy, Command command) {
    this.command = command;
    this.proxy   = proxy;

    StringBuilder sb = new StringBuilder();
    sb.append(command.name()).append(" - ").append(command.help());

    for (Argument argument : command.arguments()) {
      Arg arg = argument.annotation();
      sb.append("\n    -").append(arg.alias()).append(", --").append(arg.name());
      if (!arg.help().isEmpty()) {
        sb.append("            ").append(arg.help());
      }
      map.put("--" + argument.annotation().name(), argument);
      map.put("-" + argument.annotation().alias(), argument);
    }
    toString = sb.toString();
  }

  public Command getCommand() {
    return command;
  }

  public Object invoke(Iterator<String> iterator) {
    try {
      List<String>        errors = new ArrayList<>();
      Map<String, Object> values = new HashMap<>();
      while (iterator.hasNext()) {
        String   lexeme   = iterator.next();
        Argument argument = map.get(lexeme);
        if (argument == null) {
          errors.add("unsupported argument: " + lexeme);
          continue;
        }
        if (BooleanValueConverter.class.equals(argument.converter().getClass())) {
          values.put(argument.name(), Boolean.TRUE);
          continue;
        }
        if (!iterator.hasNext()) {
          errors.add("there is no value for argument: " + lexeme);
          continue;
        }
        String input = iterator.next();
        values.put(argument.name(), argument.converter().fromString(input));
      }

      int      index = 0;
      Object[] args  = new Object[command.arguments().size()];
      for (Argument argument : command.arguments()) {
        Object value      = values.get(argument.name());
        Arg    annotation = argument.annotation();
        if (value == null && annotation.required()) {
          errors.add(String.format("[-%s %s] is required", annotation.alias(), annotation.help()));
        } else {
          args[index] = value;
        }
        index++;
      }

      if (!errors.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("wrong usage, errors:\n");
        for (String error : errors) {
          sb.append("  ").append(error).append("\n");
        }
        throw new CommandInvocationException(sb.toString());
      }

      if (Log.debug().isEnabled(getClass())) {
        final StringBuilder msg = new StringBuilder();
        msg.append("invoking <");
        msg.append(command.method().getDeclaringClass().getSimpleName()).append('.').append(command.method().getName());
        msg.append('(');
        if (args.length > 0) {
          msg.append(args[0]);
          for (int i = 1; i < args.length; i++) {
            msg.append(", ");
            msg.append(args[i]);
          }
        }
        msg.append(")>");
        Log.debug().log(getClass(), msg.toString());
      }
      return command.method().invoke(proxy, args);
    } catch (CommandInvocationException ex) {
      throw ex;
    } catch (Throwable throwable) {
      throw new CommandInvocationException(throwable);
    }
  }

  @Override public String toString() {
    return toString;
  }
}
