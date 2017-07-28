/*
 * Copyright (C) 2017 The NightCode Open Source Project
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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * HmacSha1 implementation of an {@link AuthSigner}.
 */
public class HmacSha1AuthSigner implements AuthSigner {

  private final Mac mac;

  public HmacSha1AuthSigner(byte[] macKey) throws GeneralSecurityException {
    Objects.requireNonNull(macKey, "mac key");
    SecretKey secretKey = new SecretKeySpec(macKey, "HmacSHA1");
    mac = Mac.getInstance("HmacSHA1");
    mac.init(secretKey);
  }

  @Override public String computeSignature(String signatureBaseString) {
    return Base64.getEncoder().encodeToString(mac.doFinal(signatureBaseString.getBytes(StandardCharsets.UTF_8)));
  }

  @Override public String getSignatureMethod() {
    return "hmac-sha-1";
  }
}
