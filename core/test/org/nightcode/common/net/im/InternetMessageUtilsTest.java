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

package org.nightcode.common.net.im;

import org.junit.Assert;
import org.junit.Test;

public class InternetMessageUtilsTest {

  @Test public void testParseContentType() {
    ContentType target = InternetMessageUtils.parseContentType("text/plain; charset=us-ascii");
    Assert.assertEquals("text", target.mediaType());
    Assert.assertEquals("plain", target.subType());
    Assert.assertEquals("us-ascii", target.parameters().get("charset"));

    target = InternetMessageUtils.parseContentType(" 1adfasdfa1/X-a ; a=b; c = \"d\\\"E f\\\\\" ; charSet=\"us-ascii\"");
    Assert.assertEquals("1adfasdfa1", target.mediaType());
    Assert.assertEquals("x-a", target.subType());
    Assert.assertEquals("b", target.parameters().get("a"));
    Assert.assertEquals("d\"E f\\", target.parameters().get("c"));
    Assert.assertEquals("us-ascii", target.parameters().get("charset"));

    target = InternetMessageUtils.parseContentType("multipart/report; report-type=delivery-status;\n\tboundary=\"cd173210-7e04-4b49-bb0a-6bee344ca3a5\"");
    Assert.assertEquals("multipart", target.mediaType());
    Assert.assertEquals("report", target.subType());
    Assert.assertEquals("delivery-status", target.parameters().get("report-type"));
    Assert.assertEquals("cd173210-7e04-4b49-bb0a-6bee344ca3a5", target.parameters().get("boundary"));
  }
}
