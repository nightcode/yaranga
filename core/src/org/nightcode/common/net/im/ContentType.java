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

import java.util.Collections;
import java.util.Map;

/**
 * Content-Type header value holder.
 */
public class ContentType {

  private final String mediaType;
  private final String subType;
  private final Map<String, String> parameters;

  public ContentType(String mediaType, String subType) {
    this(mediaType, subType, Collections.emptyMap());
  }

  public ContentType(String mediaType, String subType, Map<String, String> parameters) {
    this.mediaType = mediaType;
    this.subType = subType;
    this.parameters = parameters;
  }

  public String mediaType() {
    return mediaType;
  }

  public Map<String, String> parameters() {
    return parameters;
  }

  public String subType() {
    return subType;
  }
}
