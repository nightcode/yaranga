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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CommandLineParser}.
 */
public class CommandLineParserTest {

  @Test public void test() {
    String str = "arg1 arg2  arg\\ 3 \"arg 4\"  arg\\\\5";
    
    List<String> result = CommandLineParser.parse(str);
    assertEquals(5, result.size());
    assertEquals("arg1", result.get(0));
    assertEquals("arg2", result.get(1));
    assertEquals("arg 3", result.get(2));
    assertEquals("arg 4", result.get(3));
    assertEquals("arg\\5", result.get(4));
  }
}
