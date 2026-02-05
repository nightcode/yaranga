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

import java.util.ArrayList;
import java.util.List;

enum CommandLineParser {
  ;

  private static final int M_QUOTA     = 0;
  private static final int M_SLASH     = 1;
  private static final int M_SEPARATOR = 2;
  private static final int M_SYMB      = 3;

  //                                      "  \ ' ' a
  private static final int[][] STATES = {{4, 2, 0, 5},  // quota start
                                         {4, 4, 4, 4},  // slash
                                         {0, 0, 0, 0},  // slash quota
                                         {0, 1, 4, 4},  // separator
                                         {0, 1, 3, 4},  // symbol
                                         {6, 2, 5, 5},  // quota symbol
                                         {0, 1, 3, 4}   // quota end
                                        };

  public static List<String> parse(final String line) {
    return parse(line, ' ');
  }

  private static List<String> parse(final String line, final char separator) {
    if ('"' == separator) {
      throw new IllegalStateException("Illegal separator value: " + separator);
    }
    List<String> arguments = new ArrayList<>();
    StringBuilder argument = new StringBuilder();
    char[] commandLine = line.toCharArray();

    int state = M_SYMB;
    for (char ch : commandLine) {
      state = getState(ch, state, separator);

      switch (state) {
        case 0, 1, 6 -> { }
        case 2, 4, 5 -> argument.append(ch);
        case 3 -> {
          arguments.add(argument.toString().trim());
          argument = new StringBuilder();
        }
        default -> throw new IllegalStateException("unexpected state: " + state);
      }
    }
    if (!argument.isEmpty()) {
      arguments.add(argument.toString().trim());
    }

    return arguments;
  }

  private static int getState(final char ch, final int state, final char separator) {
    int newState;
    if (ch == '"') {
      newState = STATES[state][M_QUOTA];
    } else if (ch == '\\') {
      newState = STATES[state][M_SLASH];
    } else if (ch == separator) {
      newState = STATES[state][M_SEPARATOR];
    } else {
      newState = STATES[state][M_SYMB];
    }
    return newState;
  }
}
