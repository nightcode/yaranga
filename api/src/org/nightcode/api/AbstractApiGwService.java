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
import java.util.concurrent.ExecutorService;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import org.nightcode.common.logging.Log;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.pool.metadata.InetSocketAddressEndpoint;
import org.nightcode.common.service.AbstractService;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.common.util.Throwables;

/**
 * Abstract API gateway service.
 *
 * @param <G> the gateway
 */
public abstract class AbstractApiGwService<G extends AbstractApiGw> extends AbstractService {

  private final Endpoint<InetSocketAddress> endpoint;
  private final G                gateway;
  private final ExecutorService             executor;

  public AbstractApiGwService(ApiGwConfig config) {
    int poolSize = (config.poolSize() > 0) ? config.poolSize() : Runtime.getRuntime().availableProcessors();
    executor = ExecutorUtils.fixedThreadPool(getClass().getSimpleName(), poolSize);

    SslContext sslContext = null;
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

    endpoint = new InetSocketAddressEndpoint(config.address());
    ApiGwBuilder builder = ApiGwBuilder.builder()
        .name(config.name())
        .address(endpoint.resolve())
        .sslContext(sslContext)
        .interceptor(config.interceptors().toArray(new ApiGwInterceptor[0]));

    gateway = createGateway(builder);
  }

  public void addApiHandler(ApiHandler apiHandler) {
    gateway.addApiHandler(apiHandler.name(), apiHandler);
  }

  public G gateway() {
    return gateway;
  }

  public ExecutorService executor() {
    return executor;
  }

  protected abstract G createGateway(ApiGwBuilder builder);

  @Override protected void doStart() {
    try {
      gateway.startAsync().get();
      Log.info().log(getClass(), "listening on address: {}, with SSL: {}", endpoint.resolve(), gateway.withSsl());
      notifyStarted();
    } catch (Throwable ex) {
      notifyFailed(ex);
    }
  }

  @Override protected void doStop() {
    try {
      gateway.stopAsync().get();
      ExecutorUtils.shutdown(executor);
      notifyStopped();
    } catch (Throwable ex) {
      notifyFailed(ex);
    }
  }
}
