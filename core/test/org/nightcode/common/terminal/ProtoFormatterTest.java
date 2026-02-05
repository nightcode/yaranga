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

import com.google.protobuf.Api;
import com.google.protobuf.Method;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link ProtoFormatterTest}.
 */
public class ProtoFormatterTest {

  private static final String target = """
      +---------------+-------------+---------+---------+----------------+--------+---------------+---------+
      | name          | methods     | options | version | source_context | mixins | syntax        | edition |
      +---------------+-------------+---------+---------+----------------+--------+---------------+---------+
      |               | []          | []      | 2.0     |                | []     | SYNTAX_PROTO2 |         |
      +---------------+-------------+---------+---------+----------------+--------+---------------+---------+
      | test          | [name: "A"  | []      | 1.0     |                | []     | SYNTAX_PROTO2 |         |
      |   next        | , name: "B" |         |         |                |        |               |         |
      | line          | , name: "C" |         |         |                |        |               |         |
      | one more line | , name: "D" |         |         |                |        |               |         |
      |               | ]           |         |         |                |        |               |         |
      +---------------+-------------+---------+---------+----------------+--------+---------------+---------+
      """;

  @Test public void testFormat() {
    List<Api> list = new ArrayList<>();
    list.add(Api.newBuilder().setVersion("2.0").build());
    list.add(Api.newBuilder()
        .setName("test\n  next\r\nline\rone more line")
        .addMethods(Method.newBuilder().setName("A"))
        .addMethods(Method.newBuilder().setName("B"))
        .addMethods(Method.newBuilder().setName("C"))
        .addMethods(Method.newBuilder().setName("D"))
        .setVersion("1.0").build());

    StringBuilder sb = new StringBuilder();
    ProtoFormatter.formatAsTable(list, line -> sb.append(line).append(System.lineSeparator()));
    Assert.assertEquals(target, sb.toString());
  }
}
