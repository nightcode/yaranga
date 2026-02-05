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

package org.nightcode.api.tcp;

import java.net.InetSocketAddress;
import java.net.Proxy;
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
import org.nightcode.common.pool.metadata.InetSocketAddressEndpoint;
import org.nightcode.common.props.Properties;
import org.nightcode.net.impl.ProtobufPacketReader;
import org.nightcode.net.impl.ProtobufPacketWriter;
import org.nightcode.net.impl.TcpIpPipeFactory;

/**
 * TCP/IP pool builder.
 */
public final class TcpIpApiPoolBuilder {

  private static final long CREATE_TIMEOUT_MS  = 1_000; // 1 sec.
  private static final long REBUILD_TIMEOUT_MS = 1_000; // 1 sec.

  public static TcpIpApiPoolBuilder builder() {
    return new TcpIpApiPoolBuilder();
  }

  private String                                  name;
  private Collection<Endpoint<InetSocketAddress>> endpoints;
  private SslContext                              sslContext;
  private long                                    createTimeoutMs;
  private long                                    rebuildTimeoutMs;

  private Proxy proxy = Proxy.NO_PROXY;

  private LoadBalancingPolicy loadBalancingPolicy = LoadBalancingPolicy.def();

  private TcpIpApiPoolBuilder() {
    // do nothing
  }

  public SessionPool<InetSocketAddress, ApiPipe<InetSocketAddress>> build() {
    createTimeoutMs  = Properties.instance().getLong(name + ".createTimeoutMs", CREATE_TIMEOUT_MS);
    rebuildTimeoutMs = Properties.instance().getLong(name + ".rebuildTimeoutMs", REBUILD_TIMEOUT_MS);

    TcpIpPipeFactory.Builder builder = TcpIpPipeFactory.builder()
        .packetReader(new ProtobufPacketReader<>(Response.getDefaultInstance()))
        .packetWriter(new ProtobufPacketWriter<>())
        .proxy(proxy)
        .sslContext(sslContext);

    return SessionPoolBuilder.<InetSocketAddress, ApiPipe<InetSocketAddress>>instance(name)
        .addEndpoints(endpoints)
        .loadBalancingPolicy(loadBalancingPolicy)
        .sessionFactory(builder.build())
        .createTimeout(createTimeoutMs, TimeUnit.MILLISECONDS)
        .rebuildTimeout(rebuildTimeoutMs, TimeUnit.MILLISECONDS)
        .build();
  }

  public TcpIpApiPoolBuilder address(String val) {
    endpoints = Collections.singleton(new InetSocketAddressEndpoint(val));
    return this;
  }

  public TcpIpApiPoolBuilder address(InetSocketAddress val) {
    endpoints = Collections.singleton(new InetSocketAddressEndpoint(val));
    return this;
  }

  public TcpIpApiPoolBuilder addresses(String... val) {
    endpoints = new ArrayList<>(val.length);
    for (String address : val) {
      endpoints.add(new InetSocketAddressEndpoint(address));
    }
    return this;
  }

  public TcpIpApiPoolBuilder addresses(Collection<InetSocketAddress> val) {
    endpoints = new ArrayList<>(val.size());
    for (InetSocketAddress address : val) {
      endpoints.add(new InetSocketAddressEndpoint(address));
    }
    return this;
  }

  public TcpIpApiPoolBuilder createTimeout(long val, TimeUnit unit) {
    createTimeoutMs = unit.toMillis(val);
    return this;
  }

  public TcpIpApiPoolBuilder loadBalancingPolicy(LoadBalancingPolicy val) {
    loadBalancingPolicy = Objects.requireNonNull(val, "load balancing policy");
    return this;
  }

  public TcpIpApiPoolBuilder name(String val) {
    name = Objects.requireNonNull(val, "pool name");
    return this;
  }

  public TcpIpApiPoolBuilder proxy(Proxy val) {
    proxy = Objects.requireNonNull(val, "proxy");
    return this;
  }

  public TcpIpApiPoolBuilder rebuildTimeout(long val, TimeUnit unit) {
    rebuildTimeoutMs = unit.toMillis(val);
    return this;
  }

  public TcpIpApiPoolBuilder sslContext(SslContext val) {
    sslContext = val;
    return this;
  }
}
