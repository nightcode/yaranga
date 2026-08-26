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

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.netty.handler.ssl.SslContext;
import org.nightcode.api.ApiPipe;
import org.nightcode.api.message.Response;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.SessionPoolBuilder;
import org.nightcode.common.pool.lb.LoadBalancingPolicy;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.pool.metadata.UriEndpoint;
import org.nightcode.common.props.Properties;
import org.nightcode.net.BootstrapFactory;
import org.nightcode.net.impl.ProtobufPacketReader;
import org.nightcode.net.impl.ProtobufPacketWriter;

/**
 * HTTP pool builder.
 */
public final class HttpApiPoolBuilder {

  private static final long CREATE_TIMEOUT_MS  = 1_000; // 1 sec.
  private static final long REBUILD_TIMEOUT_MS = 5_000; // 5 sec.

  public static HttpApiPoolBuilder builder() {
    return new HttpApiPoolBuilder();
  }

  private String                    name;
  private Collection<Endpoint<URI>> endpoints;
  private SslContext                sslContext;
  private long                      createTimeoutMs;
  private long                      rebuildTimeoutMs;

  private Proxy proxy = Proxy.NO_PROXY;

  private LoadBalancingPolicy loadBalancingPolicy = LoadBalancingPolicy.def();

  private HttpApiPoolBuilder() {
    // do nothing
  }

  public SessionPool<URI, ApiPipe<URI>> build() {
    createTimeoutMs  = Properties.instance().getLong(name + ".createTimeoutMs", CREATE_TIMEOUT_MS);
    rebuildTimeoutMs = Properties.instance().getLong(name + ".rebuildTimeoutMs", REBUILD_TIMEOUT_MS);

    HttpApiPipeFactory.Builder builder = HttpApiPipeFactory.builder()
        .bootstrapFactory(BootstrapFactory.tcpIpFactory().withSsl(sslContext))
        .packetReader(new ProtobufPacketReader<>(Response.getDefaultInstance()))
        .packetWriter(new ProtobufPacketWriter<>())
        .proxy(proxy);

    return SessionPoolBuilder.<URI, ApiPipe<URI>>instance(name)
        .addEndpoints(endpoints)
        .loadBalancingPolicy(loadBalancingPolicy)
        .sessionFactory(builder.build())
        .createTimeout(createTimeoutMs, TimeUnit.MILLISECONDS)
        .rebuildTimeout(rebuildTimeoutMs, TimeUnit.MILLISECONDS)
        .build();
  }

  public HttpApiPoolBuilder uri(String val) {
    try {
      endpoints = Collections.singleton(new UriEndpoint(new URI(val)));
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    }
    return this;
  }

  public HttpApiPoolBuilder uri(URI val) {
    endpoints = Collections.singleton(new UriEndpoint(val));
    return this;
  }

  public HttpApiPoolBuilder uri(String... val) {
    endpoints = new ArrayList<>(val.length);
    for (String address : val) {
      try {
        endpoints.add(new UriEndpoint(new URI(address)));
      } catch (URISyntaxException ex) {
        throw new IllegalArgumentException(ex);
      }
    }
    return this;
  }

  public HttpApiPoolBuilder uri(Collection<URI> val) {
    endpoints = new ArrayList<>(val.size());
    for (URI address : val) {
      endpoints.add(new UriEndpoint(address));
    }
    return this;
  }

  public HttpApiPoolBuilder createTimeout(long val, TimeUnit unit) {
    createTimeoutMs = unit.toMillis(val);
    return this;
  }

  public HttpApiPoolBuilder loadBalancingPolicy(LoadBalancingPolicy val) {
    loadBalancingPolicy = Objects.requireNonNull(val, "load balancing policy");
    return this;
  }

  public HttpApiPoolBuilder name(String val) {
    name = Objects.requireNonNull(val, "pool name");
    return this;
  }

  public HttpApiPoolBuilder proxy(Proxy val) {
    proxy = Objects.requireNonNull(val, "proxy");
    return this;
  }

  public HttpApiPoolBuilder rebuildTimeout(long val, TimeUnit unit) {
    rebuildTimeoutMs = unit.toMillis(val);
    return this;
  }

  public HttpApiPoolBuilder sslContext(SslContext val) {
    sslContext = val;
    return this;
  }
}
