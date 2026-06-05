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

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.Nullable;
import org.nightcode.api.http.HttpApiGw;
import org.nightcode.api.message.Request;
import org.nightcode.api.tcp.TcpIpApiGw;
import org.nightcode.common.util.Throwables;
import org.nightcode.net.BootstrapServerFactory;
import org.nightcode.net.SslContextConfig;

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

  String                    name;
  BootstrapServerFactory<?> bootstrapFactory;
  SslContext                sslContext;

  int                       maxBodyLengthBytes  = 1024 * 1024; // 1Mb
  List<ApiGwInterceptor>    interceptors        = Collections.emptyList();
  Function<Request, String> serviceNameProvider = DEF_SERVICE_NAME_PROVIDER;

  private ApiGwBuilder() {
    // do nothing
  }

  public ApiGw buildHttpApiGw() {
    return new HttpApiGw(this);
  }

  public ApiGw buildTcpIpApiGw() {
    return new TcpIpApiGw(this);
  }

  public <A extends SocketAddress> ApiGwBuilder bootstrapFactory(BootstrapServerFactory<A> val) {
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

  public ApiGwBuilder sslContext(SslContextConfig config) {
    if (config.useSsl()) {
      char[] keystorePasswd   = config.keystorePassword().toCharArray();
      char[] truststorePasswd = config.truststorePassword().toCharArray();

      try {
        sslContext = SslContextBuilder.builder()
            .keyManager(keystorePasswd, config.keystorePath())
            .trustManager(truststorePasswd, config.truststorePath())
            .clientAuth(ClientAuth.REQUIRE)
            .buildForServer();
      } catch (Exception ex) {
        throw Throwables.rethrow(ex);
      }
    }
    return this;
  }
}
