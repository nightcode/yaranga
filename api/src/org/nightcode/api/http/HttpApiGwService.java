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

package org.nightcode.api.http;

import org.nightcode.api.AbstractApiGwService;
import org.nightcode.api.ApiGwBuilder;
import org.nightcode.api.ApiGwConfig;

/**
 * HTTP API gateway service.
 */
public class HttpApiGwService extends AbstractApiGwService<HttpApiGw> {

  public HttpApiGwService(ApiGwConfig config) {
    super(config);
  }

  @Override protected HttpApiGw createGateway(ApiGwBuilder builder) {
    return HttpApiGw.build(builder);
  }
}
