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

package org.nightcode.api;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.Nullable;
import org.nightcode.api.message.Request;
import org.nightcode.net.BootstrapServerFactory;

/**
 * API gateway builder.
 */
public final class ApiGwBuilder {

  public static ApiGwBuilder builder() {
    return new ApiGwBuilder();
  }

  private static final Function<Request, String> DEF_SERVICE_NAME_PROVIDER = request -> {
    String typeUrl = request.getPayload().getTypeUrl();
    int    pos     = typeUrl.indexOf('/');
    if (pos == -1) {
      return typeUrl;
    }
    return typeUrl.substring(0, pos);
  };

  String            name;
  InetSocketAddress address;
  SslContext        sslContext;

  BootstrapServerFactory bootstrapFactory = BootstrapServerFactory.tcpIpServerFactory();

  int                       maxBodyLengthBytes  = 1024 * 1024; // 1Mb
  List<ApiGwInterceptor>    interceptors        = Collections.emptyList();
  Function<Request, String> serviceNameProvider = DEF_SERVICE_NAME_PROVIDER;

  private ApiGwBuilder() {
    // do nothing
  }

  public ApiGwBuilder address(InetSocketAddress val) {
    Objects.requireNonNull(val, "address");
    address = val;
    return this;
  }

  public ApiGwBuilder bootstrapFactory(BootstrapServerFactory val) {
    Objects.requireNonNull(val, "BootstrapServerFactory");
    bootstrapFactory = val;
    return this;
  }

  public ApiGwBuilder interceptor(ApiGwInterceptor... val) {
    interceptors = Arrays.asList(val);
    return this;
  }

  public ApiGwBuilder maxBodyLengthBytes(int val) {
    maxBodyLengthBytes = val;
    return this;
  }

  public ApiGwBuilder name(String val) {
    Objects.requireNonNull(val, "name");
    name = val;
    return this;
  }

  public ApiGwBuilder serviceNameProvider(Function<Request, String> val) {
    Objects.requireNonNull(val, "service name provider");
    serviceNameProvider = val;
    return this;
  }

  public ApiGwBuilder sslContext(@Nullable SslContext val) {
    sslContext = val;
    return this;
  }
}
