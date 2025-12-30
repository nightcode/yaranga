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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Auth helper class.
 */
public final class AuthUtils {

  /**
   * Returns an encoded string.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.6">3.6. Percent Encoding</a>
   *
   * @param source source string for encoding
   * @return encoded string
   */
  public static String percentEncode(String source) {
    return URLEncoder.encode(source, StandardCharsets.UTF_8)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~");
  }

  /**
   * Returns a decoded string.
   *
   * @param source source string for decoding
   * @return decoded string
   */
  public static String percentDecode(String source) {
    return URLDecoder.decode(source, StandardCharsets.UTF_8);
  }

  // Suppress default constructor for noninstantiability.
  private AuthUtils() {
    throw new AssertionError();
  }
}
