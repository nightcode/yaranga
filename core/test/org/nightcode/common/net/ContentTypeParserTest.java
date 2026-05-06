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

package org.nightcode.common.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentTypeParserTest {

  @Test public void testParseContentType() {
    ContentType target = ContentTypeParser.parse("text/plain");
    assertEquals("text", target.mediaType());
    assertEquals("plain", target.subType());
    assertTrue(target.parameters().isEmpty());

    target = ContentTypeParser.parse("text/plain; charset=us-ascii");
    assertEquals("text", target.mediaType());
    assertEquals("plain", target.subType());
    assertEquals("us-ascii", target.parameters().get("charset"));

    target = ContentTypeParser.parse(" 1adfasdfa1/X-a ; a=b; c = \"d\\\"E f\\\\\" ; charSet=\"us-ascii\"");
    assertEquals("1adfasdfa1", target.mediaType());
    assertEquals("x-a", target.subType());
    assertEquals("b", target.parameters().get("a"));
    assertEquals("d\"E f\\", target.parameters().get("c"));
    assertEquals("us-ascii", target.parameters().get("charset"));

    target = ContentTypeParser.parse("multipart/report; report-type=delivery-status;\n\tboundary=\"cd173210-7e04-4b49-bb0a-6bee344ca3a5\"");
    assertEquals("multipart", target.mediaType());
    assertEquals("report", target.subType());
    assertEquals("delivery-status", target.parameters().get("report-type"));
    assertEquals("cd173210-7e04-4b49-bb0a-6bee344ca3a5", target.parameters().get("boundary"));
  }
}
