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

import org.nightcode.common.base.Hexs;
import org.nightcode.common.net.http.AuthSigner;
import org.nightcode.common.net.http.HmacSha256AuthSigner;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.Test;

public class HmacSha256AuthSignerTest {

  @Test public void computeSignature() throws GeneralSecurityException {
    AuthSigner signer = new HmacSha256AuthSigner("8yfrufh348h".getBytes(StandardCharsets.UTF_8));
    String signatureBaseString = "273156:di3hvdf8\nPOST\n/request\nexample.com\n80\nk9kbtCIy0CkI3/FEfpS/oIDjk6k=\n\n";

    byte[] buffer = signer.computeSignature(signatureBaseString.getBytes(StandardCharsets.UTF_8));
    Assert.assertArrayEquals(Hexs.hex().toByteArray("A2E5D351094523EA49322DB39FAEDAEB39ACEEC2EED891143D4633BA712A28E7")
        , buffer);
    
    String actual = signer.computeSignatureBase64(signatureBaseString.getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals("ouXTUQlFI+pJMi2zn67a6zms7sLu2JEUPUYzunEqKOc=", actual);

    actual = signer.computeSignatureHex(signatureBaseString.getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals("A2E5D351094523EA49322DB39FAEDAEB39ACEEC2EED891143D4633BA712A28E7", actual);
  }
}
