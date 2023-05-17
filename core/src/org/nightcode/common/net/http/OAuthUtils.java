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

import org.nightcode.common.base.StringIterator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

/**
 * OAuth helper class.
 */
public final class OAuthUtils {

  private static class RequestParameter implements Comparable<RequestParameter> {

    private final String encodedName;
    private final String encodedValue;

    RequestParameter(String name) throws AuthException {
      this(name, null);
    }

    RequestParameter(String name, String value) throws AuthException {
      this.encodedName = AuthUtils.percentEncode(name);
      this.encodedValue = value == null ? "" : AuthUtils.percentEncode(value);
    }

    public String getEncodedName() {
      return encodedName;
    }

    public String getEncodedValue() {
      return encodedValue;
    }

    @Override public int compareTo(@NotNull RequestParameter requestParameter) {
      int result = encodedName.compareTo(requestParameter.encodedName);
      if (result != 0) {
        return  result;
      } else {
        return encodedValue.compareTo(requestParameter.encodedValue);
      }
    }

    @Override public int hashCode() {
      int result = 527 + encodedName.hashCode();
      return result * 31 + encodedValue.hashCode();
    }

    @Override public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof RequestParameter)) {
        return false;
      }
      RequestParameter other = (RequestParameter) obj;
      return Objects.equals(encodedName, other.encodedName)
          && Objects.equals(encodedValue, other.encodedValue);
    }
  }

  /**
   * Returns a nonce value. The nonce value must be unique
   * across all requests with the same timestamp,
   * client credentials, and token combinations.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.3">3.3 Nonce and Timestamp</a>
   *
   * @return nonce value
   */
  public static String getNonce() {
    return Long.toString(System.nanoTime());
  }

  /**
   * Returns a signature base string. The signature base string
   * is a consistent, reproducible concatenation of several of
   * the HTTP request elements into a single string.
   *
   * The signature base string includes the following components of the HTTP request:
   * <ul>
   *   <li>the HTTP request method (e.g., "GET", "POST", etc.)</li>
   *   <li>the authority as declared by the HTTP "Host" request header field</li>
   *   <li>the path and query components of the request resource URI</li>
   *   <li>the protocol parameters excluding the "oauth_signature"</li>
   *   <li>parameters included in the request entity-body if they comply with
           the strict restrictions defined in Section 3.4.1.3</li>
   * </ul>
   *
   * NOTE 1: only 4 first parameter sources are used for now.
   * NOTE 2: no percent encoding for custom HTTP methods for now.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.1">3.4.1.
   *      Signature Base String</a>
   *
   * @param requestMethod request method
   * @param requestUrl request url
   * @param protocolParameters protocol parameters
   * @return signature base string
   * @throws AuthException if some of parameters has unacceptable value
   */
  public static String getSignatureBaseString(String requestMethod, String requestUrl,
      Map<String, String> protocolParameters) throws AuthException {
    StringBuilder sb = new StringBuilder();
    sb.append(requestMethod.toUpperCase()).append("&")
        .append(AuthUtils.percentEncode(normalizeUrl(requestUrl))).append("&")
        .append(AuthUtils.percentEncode(normalizeParameters(requestUrl, protocolParameters)));
    return sb.toString();
  }

  /**
   * Returns a timestamp value. The timestamp is expressed
   * in the number of seconds since January 1, 1970 00:00:00 GMT.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.3">3.3 Nonce and Timestamp</a>
   *
   * @return timestamp value
   */
  public static String getTimestamp() {
    return Long.toString(System.currentTimeMillis() / 1000);
  }

  /**
   * Returns normalized request parameters. The parameters are normalized
   * into a single string as follows:
   *  1.  First, the name and value of each parameter are encoded.
   *  2.  The parameters are sorted by name, using ascending byte
   *      value ordering. If two or more parameters share the same name,
   *      they are sorted by their value.
   *  3.  The name of each parameter is concatenated to its corresponding
   *      value using an "=" character (ASCII code 61) as a separator,
   *      even if the value is empty.
   *  4.  The sorted name/value pairs are concatenated together into a
   *      single string by using an "&amp;" character (ASCII code 38) as separator.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.1.3">3.4.1.3.2.
   *      Parameters Normalization</a>
   *
   * @param requestUrl request url
   * @param protocolParameters oauth protocol parameters
   * @return the normalized request parameters
   * @throws AuthException an authentication exception
   */
  public static String normalizeParameters(String requestUrl,
      Map<String, String> protocolParameters) throws AuthException {
    SortedSet<RequestParameter> parameters = new TreeSet<>();
    int index = requestUrl.indexOf('?');
    if (index > 0) {
      String query = requestUrl.substring(index + 1);
      Iterator<String> i = new StringIterator(query, "&");
      while (i.hasNext()) {
        String parameter = i.next();
        int equalsIndex = parameter.indexOf('=');
        if (equalsIndex > 0) {
          parameters.add(new RequestParameter(AuthUtils.percentDecode(parameter.substring(0, equalsIndex))
              , AuthUtils.percentDecode(parameter.substring(equalsIndex + 1))));
        } else if (equalsIndex == -1) {
          parameters.add(new RequestParameter(AuthUtils.percentDecode(parameter)));
        }
      }
    }

    for (Entry<String, String> entry : protocolParameters.entrySet()) {
      parameters.add(new RequestParameter(entry.getKey(), entry.getValue()));
    }

    StringBuilder normalized = new StringBuilder();
    Iterator<RequestParameter> parameterIterator = parameters.iterator();
    if (parameterIterator.hasNext()) {
      RequestParameter requestParameter = parameterIterator.next();
      normalized.append(requestParameter.getEncodedName())
          .append('=').append(requestParameter.getEncodedValue());
      while (parameterIterator.hasNext()) {
        requestParameter = parameterIterator.next();
        normalized.append('&').append(requestParameter.getEncodedName())
            .append('=').append(requestParameter.getEncodedValue());
      }
    }

    return normalized.toString();
  }

  /**
   * Returns a normalized request uri. The scheme, authority, and path
   * of the request resource uri [rfc 3986] are included by constructing
   * an "http" or "https" uri representing the request resource
   * (without the query or fragment).
   *
   * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.1">3.4.1.2. Base String URI</a>
   *
   * @param requestUrl request url
   * @return the normalized request uri
   * @throws AuthException if the <code>requestUrl</code> violates rfc 2396
   */
  public static String normalizeUrl(String requestUrl) throws AuthException {
    URI uri;
    try {
      uri = new URI(requestUrl);
    } catch (URISyntaxException ex) {
      throw new AuthException(ex);
    }
    String scheme = uri.getScheme();
    String authority = uri.getAuthority();
    if (scheme == null || authority == null) {
      throw new AuthException("Invalid requestUrl [" + requestUrl + "].");
    }
    scheme = scheme.toLowerCase();
    authority = authority.toLowerCase();

    if (("http".equals(scheme) && uri.getPort() == 80)
        || ("https".equals(scheme) && uri.getPort() == 443)) {
      int colonIndex = authority.indexOf(":");
      if (colonIndex > 0) {
        authority = authority.substring(0, colonIndex);
      }
    }
    String path = uri.getRawPath();
    if (path == null || path.length() == 0) {
      path = "/";
    }

    return scheme + "://" + authority + path;
  }

  // Suppress default constructor for noninstantiability.
  private OAuthUtils() {
    throw new AssertionError();
  }
}
