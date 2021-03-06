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
import org.nightcode.common.net.http.AuthUtils;
import org.nightcode.common.net.http.OAuthUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link OAuthUtils}.
 */
public class OAuthUtilsTest {

  @Test public void test() throws AuthException {
    String str = "http://example.com /request?b5=%3D%253D&a3=a&c%40=&a2=r%20b&c2&a3=2+q";
    String strEncoded = AuthUtils.percentEncode(str);
    String strDecoded = AuthUtils.percentDecode(strEncoded);

    Assert.assertEquals("http%3A%2F%2Fexample.com%20%2Frequest%3Fb5%3D%253D%25253D%26a3%3Da%26c%2540"
        + "%3D%26a2%3Dr%2520b%26c2%26a3%3D2%2Bq", strEncoded);
    Assert.assertEquals(str, strDecoded);
  }

  @Test public void testGetSignatureBaseString() throws AuthException {
    String requestMethod = "POST";
    String requestUrl = "http://example.com/request?b5=%3D%253D&a3=a&c%40=&a2=r%20b&c2&a3=2+q";
    Map<String, String> protocolParameters = new HashMap<String, String>();
    protocolParameters.put("oauth_consumer_key", "9djdj82h48djs9d2");
    protocolParameters.put("oauth_token", "kkk9d7dh3k39sjv7");
    protocolParameters.put("oauth_signature_method", "HMAC-SHA1");
    protocolParameters.put("oauth_timestamp", "137131201");
    protocolParameters.put("oauth_nonce", "7d8f3e4a");

    String signatureBaseString = OAuthUtils.getSignatureBaseString(requestMethod, requestUrl
        , protocolParameters);
    String expectedSignatureBaseString = "POST&http%3A%2F%2Fexample.com%2Frequest&a2%3Dr%2520b"
        + "%26a3%3D2%2520q%26a3%3Da%26b5%3D%253D%25253D%26c%2540%3D%26c2%3D%26"
        + "oauth_consumer_key%3D9djdj82h48djs9d2%26oauth_nonce%3D7d8f3e4a%26"
        + "oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D137131201%26"
        + "oauth_token%3Dkkk9d7dh3k39sjv7";

    Assert.assertEquals(expectedSignatureBaseString, signatureBaseString);
  }

  @Test public void testGetNonce() {
    Assert.assertNotSame(OAuthUtils.getNonce(), OAuthUtils.getNonce());
  }

  @Test public void testGetTimestamp() {
    Assert.assertNotNull(OAuthUtils.getTimestamp());
  }

  @Test public void testNormalizeParameters() {
    String requestUrl = "http://example.com/request?b5=%3D%253D&a3=a&c%40=&a2=r%20b&c2&a3=2+q";
    Map<String, String> protocolParameters = new HashMap<String, String>();
    protocolParameters.put("oauth_consumer_key", "9djdj82h48djs9d2");
    protocolParameters.put("oauth_token", "kkk9d7dh3k39sjv7");
    protocolParameters.put("oauth_signature_method", "HMAC-SHA1");
    protocolParameters.put("oauth_timestamp", "137131201");
    protocolParameters.put("oauth_nonce", "7d8f3e4a");

    String expectedNormalizeParameters = "a2=r%20b&a3=2%20q&a3=a&b5=%3D%253D&c%40=&c2="
        + "&oauth_consumer_key=9djdj82h48djs9d2&oauth_nonce=7d8f3e4a"
        + "&oauth_signature_method=HMAC-SHA1&oauth_timestamp=137131201"
        + "&oauth_token=kkk9d7dh3k39sjv7";

    try {
      Assert.assertEquals(expectedNormalizeParameters
          , OAuthUtils.normalizeParameters(requestUrl, protocolParameters));
    } catch (AuthException ex) {
      Assert.fail(ex.getMessage());
    }
  }

  @Test public void testNormalizeUrl() throws AuthException {
    String sourceUrl1 = "HTTP://EXAMPLE.COM:80/r%20v/X?id=123";
    String expectedUrl1 = "http://example.com/r%20v/X";

    String sourceUrl2 = "HTTP://www.example.net:8080/?q=1";
    String expectedUrl2 = "http://www.example.net:8080/";

    String sourceUrl3 = "HTTPS://www.example.net:443";
    String expectedUrl3 = "https://www.example.net/";

    Assert.assertEquals(expectedUrl1, OAuthUtils.normalizeUrl(sourceUrl1));
    Assert.assertEquals(expectedUrl2, OAuthUtils.normalizeUrl(sourceUrl2));
    Assert.assertEquals(expectedUrl3, OAuthUtils.normalizeUrl(sourceUrl3));
  }

  @Test public void testNormalizeUrlURISyntax() {
    try {
      OAuthUtils.normalizeUrl("http://uri<>syntax");
    } catch (AuthException ex) {
      Assert.assertFalse(ex.getMessage().contains("Invalid requestUrl"));
      return;
    }
    Assert.fail();
  }

  @Test public void testNormalizeInvalidUrl() {
    try {
      OAuthUtils.normalizeUrl("InvalidRequestUrl");
    } catch (AuthException ex) {
      Assert.assertTrue(ex.getMessage().contains("Invalid requestUrl"));
      return;
    }
    Assert.fail();
  }
}
