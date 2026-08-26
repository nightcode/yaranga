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
import java.util.Objects;

import org.nightcode.api.ApiPipe;
import org.nightcode.common.pool.SessionContext;
import org.nightcode.common.pool.SessionFactory;
import org.nightcode.net.BootstrapFactory;
import org.nightcode.net.PacketReader;
import org.nightcode.net.PacketWriter;
import org.nightcode.net.PipeContext;
import org.nightcode.net.PipeFactoryContext;
import org.nightcode.net.impl.PipeContextImpl;

/**
 * HTTP connection factory.
 */
public final class HttpApiPipeFactory implements SessionFactory<URI, ApiPipe<URI>>, PipeFactoryContext {

  public static final class Builder {
    private BootstrapFactory bootstrapFactory;
    private PacketReader<?>  packetReader;
    private PacketWriter<?>  packetWriter;

    private Proxy proxy = Proxy.NO_PROXY;

    private int maxBodyLengthBytes = 1024 * 1024;
    private int nThreads           = Runtime.getRuntime().availableProcessors();

    private boolean autoRead       = true;
    private boolean soReuseAddress = true;
    private boolean soKeepAlive    = true;
    private boolean tcpNoDelay     = true;

    private Builder() {
      // do nothing
    }

    public Builder autoRead(boolean val) {
      autoRead = val;
      return this;
    }

    public Builder bootstrapFactory(BootstrapFactory val) {
      bootstrapFactory = Objects.requireNonNull(val, "bootstrap factory");
      return this;
    }

    public HttpApiPipeFactory build() {
      return new HttpApiPipeFactory(this);
    }

    public Builder maxBodyLengthBytes(int val) {
      maxBodyLengthBytes = val;
      return this;
    }

    public Builder nThreads(int val) {
      nThreads = val;
      return this;
    }

    public Builder packetReader(PacketReader<?> val) {
      packetReader = Objects.requireNonNull(val, "packet reader");
      return this;
    }

    public Builder packetWriter(PacketWriter<?> val) {
      packetWriter = Objects.requireNonNull(val, "packet writer");
      return this;
    }

    public Builder proxy(Proxy val) {
      proxy = Objects.requireNonNull(val, "proxy");
      return this;
    }

    public Builder soReuseAddress(boolean val) {
      soReuseAddress = val;
      return this;
    }

    public Builder soKeepAlive(boolean val) {
      soKeepAlive = val;
      return this;
    }

    public Builder tcpNoDelay(boolean val) {
      tcpNoDelay = val;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final boolean autoRead;
  private final boolean soReuseAddress;
  private final boolean soKeepAlive;
  private final boolean tcpNoDelay;

  private final int maxBodyLengthBytes;
  private final int nThreads;

  private final Proxy            proxy;
  private final PacketReader<?>  packetReader;
  private final PacketWriter<?>  packetWriter;
  private final BootstrapFactory bootstrapFactory;

  private HttpApiPipeFactory(Builder builder) {
    autoRead           = builder.autoRead;
    soKeepAlive        = builder.soKeepAlive;
    soReuseAddress     = builder.soReuseAddress;
    tcpNoDelay         = builder.tcpNoDelay;
    maxBodyLengthBytes = builder.maxBodyLengthBytes;
    nThreads           = builder.nThreads;
    proxy              = builder.proxy;
    packetReader       = builder.packetReader;
    packetWriter       = builder.packetWriter;
    bootstrapFactory   = builder.bootstrapFactory;
  }

  @Override public boolean autoRead() {
    return autoRead;
  }

  @Override public BootstrapFactory bootstrapFactory() {
    return bootstrapFactory;
  }

  @Override public ApiPipe<URI> create(SessionContext<URI, ApiPipe<URI>> context) {
    PipeContext<URI, ApiPipe<URI>> connectionContext = new PipeContextImpl<>(context, this);
    HttpApiPipe                    connection        = new HttpApiPipe(connectionContext);

    connection.addListener(context.pool());
    context.initialize(connection);
    return connection;
  }

  @Override public int maxBodyLengthBytes() {
    return maxBodyLengthBytes;
  }

  @Override public int nThreads() {
    return nThreads;
  }

  @Override public <M> PacketReader<M> packetReader() {
    // noinspection unchecked
    return (PacketReader<M>) packetReader;
  }

  @Override public <M> PacketWriter<M> packetWriter() {
    // noinspection unchecked
    return (PacketWriter<M>) packetWriter;
  }

  @Override public Proxy proxy() {
    return proxy;
  }

  @Override public boolean soKeepAlive() {
    return soKeepAlive;
  }

  @Override public boolean soReuseAddress() {
    return soReuseAddress;
  }

  @Override public boolean tcpNoDelay() {
    return tcpNoDelay;
  }
}
