/*
 * Copyright (C) 2008 The NightCode Open Source Project
 *
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

/**
 * Objects helper class.
 */
public final class Objects {

  private Objects() {
    // do nothing
  }

  /**
   * Checks boolean expression.
   *
   * @param expression a boolean expression
   * @param message error message
   * @throws IllegalArgumentException if boolean expression is false
   */
  public static void validArgument(boolean expression, String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Checks boolean expression.
   *
   * @param expression a boolean expression
   * @param message error message
   * @param messageArgs array of parameters to the message
   * @throws IllegalArgumentException if boolean expression is false
   */
  public static void validArgument(boolean expression, String message, Object... messageArgs) {
    if (!expression) {
      throw new IllegalArgumentException(format(message, messageArgs));
    }
  }

  /**
   * Checks boolean expression.
   *
   * @param expression a boolean expression
   * @param message error message
   * @throws IllegalArgumentException if boolean expression is false
   */
  public static void validState(boolean expression, String message) {
    if (!expression) {
      throw new IllegalStateException(message);
    }
  }

  /**
   * Checks boolean expression.
   *
   * @param expression a boolean expression
   * @param message error message
   * @param messageArgs array of parameters to the message
   * @throws IllegalArgumentException if boolean expression is false
   */
  public static void validState(boolean expression, String message, Object... messageArgs) {
    if (!expression) {
      throw new IllegalStateException(format(message, messageArgs));
    }
  }

  /**
   * Returns a formatted string using the specified format string and arguments.
   *
   * @param message a format string
   * @param messageArgs arguments referenced by the format specifiers in the format string.
   *        If there are more arguments than format specifiers, the extra arguments are ignored.
   *        The number of arguments is variable and may be zero.
   * @return a formatted string
   */
  private static String format(String message, Object... messageArgs) {
    StringBuilder sb = new StringBuilder(message.length() + 8 * messageArgs.length);
    int templateStart = 0;
    for (Object argument : messageArgs) {
      int placeholderStart = message.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      sb.append(message.substring(templateStart, placeholderStart));
      sb.append(argument);
      templateStart = placeholderStart + 2;
    }
    sb.append(message.substring(templateStart));
    return sb.toString();
  }
}
