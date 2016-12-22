/*
 * Copyright (C) 2016 The NightCode Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.nightcode.common.net;

import java.security.GeneralSecurityException;

import org.junit.Test;

import junit.framework.Assert;

public class HmacSha256AuthSignerTest {

  @Test public void computeSignature() throws GeneralSecurityException {
    AuthSigner signer = new HmacSha256AuthSigner("8yfrufh348h");
    String signatureBaseString = "273156:di3hvdf8\nPOST\n/request\nexample.com\n80\nk9kbtCIy0CkI3/FEfpS/oIDjk6k=\n\n";
    
    String actual = signer.computeSignature(signatureBaseString);
    Assert.assertEquals("ouXTUQlFI+pJMi2zn67a6zms7sLu2JEUPUYzunEqKOc=", actual);
  }
}
