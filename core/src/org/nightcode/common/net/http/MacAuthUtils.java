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

package org.nightcode.common.net.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

/**
 * MAC authentication helper class.
 */
public final class MacAuthUtils {

  /**
   * Returns a nonce value. The nonce value MUST be unique
   * across all requests with the same MAC key identifier.
   *
   * @see <a href="https://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05#section-3.1">3.1.
   *      The "Authorization" Request Header</a>
   *
   * @param issueTime the number of seconds since the credentials were issued to the client
   *
   * @return nonce value
   */
  public static String getNonce(long issueTime) {
    long currentTime = new Date().getTime();
    return TimeUnit.MILLISECONDS.toSeconds(currentTime - issueTime) + ":" + Long.toString(System.nanoTime());
  }

  /**
   * Returns a signature base string.
   *
   * The signature base string is constructed by concatenating together, in order, the
   * following HTTP request elements, each followed by a new line character (%x0A):
   *   1.  The nonce value generated for the request.
   *   2.  The HTTP request method in upper case.  For example: "HEAD",
   *       "GET", "POST", etc.
   *   3.  The HTTP request-URI as defined by [RFC2616] section 5.1.2.
   *   4.  The hostname included in the HTTP request using the "Host"
   *       request header field in lower case.
   *   5.  The port as included in the HTTP request using the "Host" request
   *       header field.  If the header field does not include a port, the
   *       default value for the scheme MUST be used (e.g. 80 for HTTP and
   *       443 for HTTPS).
   *   6.  The request payload body hash as described in Section 3.2 if one
   *       was calculated and included in the request, otherwise, an empty
   *       string.  Note that the body hash of an empty payload body is not
   *       an empty string.
   *   7.  The value of the "ext" "Authorization" request header field
   *       attribute if one was included in the request, otherwise, an empty
   *       string.
   * Each element is followed by a new line character (%x0A) including the
   * last element and even when an element value is an empty string.
   *
   * @see <a href="https://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05#section-3.3.1">3.3.1.
   *      Normalized Request String</a>
   *
   * @param nonce the nonce value
   * @param requestMethod the request method
   * @param headerHost the "Host" request header field value
   * @param requestUrl request url
   * @param payloadBodyHash the request payload body hash
   * @param ext the "ext" "Authorization" request header field attribute
   * @return signature base string
   * @throws AuthException if some of parameters has unacceptable value
   */
  public static String getSignatureBaseString(String nonce, String requestMethod, String headerHost, String requestUrl,
      @Nullable String payloadBodyHash, @Nullable String ext) throws AuthException {
    return nonce + '\n'
        + requestMethod.toUpperCase() + '\n'
        + normalizeUrl(headerHost, requestUrl) + '\n'
        + (payloadBodyHash != null ? payloadBodyHash : "") + '\n'
        + (ext != null ? ext : "") + '\n';
  }

  private static String normalizeUrl(String headerHost, String requestUrl) throws AuthException {
    URI uri;
    try {
      uri = new URI(requestUrl);
    } catch (URISyntaxException ex) {
      throw new AuthException(ex);
    }
    String path = uri.getRawPath();
    if (path == null || path.length() == 0) {
      path = "/";
    }
    String host;
    String port;
    int separator = headerHost.indexOf(':');
    if (separator == -1) {
      host = headerHost;
      port = ("http".equals(uri.getScheme())) ? "80" : "443";
    } else {
      host = headerHost.substring(0, separator);
      port = headerHost.substring(separator + 1).trim();
    }
    return path + "\n" + host.trim().toLowerCase() + "\n" + port;
  }

  // Suppress default constructor for noninstantiability.
  private MacAuthUtils() {
    throw new AssertionError();
  }
}
