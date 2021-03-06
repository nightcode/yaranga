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

import org.nightcode.common.net.http.AuthException;
import org.nightcode.common.net.http.MacAuthUtils;

import org.junit.Assert;
import org.junit.Test;

public class MacAuthUtilsTest {

  @Test public void getSignatureBaseString() throws AuthException {
    String actual = MacAuthUtils.getSignatureBaseString("273156:di3hvdf8", "POST", "example.com"
        , "http://example.com/request", "k9kbtCIy0CkI3/FEfpS/oIDjk6k=", null);
    Assert.assertEquals("273156:di3hvdf8\nPOST\n/request\nexample.com\n80\nk9kbtCIy0CkI3/FEfpS/oIDjk6k=\n\n", actual);

    actual = MacAuthUtils.getSignatureBaseString("273156:di3hvdf8", "POST", "localhost:8080"
        , "http://localhost:8080/request", "k9kbtCIy0CkI3/FEfpS/oIDjk6k=", null);
    Assert.assertEquals("273156:di3hvdf8\nPOST\n/request\nlocalhost\n8080\nk9kbtCIy0CkI3/FEfpS/oIDjk6k=\n\n", actual);

    actual = MacAuthUtils.getSignatureBaseString("273156:di3hvdf8", "POST", "localhost"
        , "https://localhost:443/request", "k9kbtCIy0CkI3/FEfpS/oIDjk6k=", null);
    Assert.assertEquals("273156:di3hvdf8\nPOST\n/request\nlocalhost\n443\nk9kbtCIy0CkI3/FEfpS/oIDjk6k=\n\n", actual);
  }
}
