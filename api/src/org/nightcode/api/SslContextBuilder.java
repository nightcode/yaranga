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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Objects;
import java.util.function.Consumer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import org.nightcode.common.util.Throwables;

/**
 * SSL context builder.
 */
public final class SslContextBuilder {

  public static SslContextBuilder builder() {
    return new SslContextBuilder();
  }

  private KeyManagerFactory   keyManagerFactory;
  private TrustManagerFactory trustManagerFactory;
  private ClientAuth          clientAuth;

  private SslContextBuilder() {
    // do nothing
  }

  public SslContext buildForClient() {
    try {
      io.netty.handler.ssl.SslContextBuilder builder = io.netty.handler.ssl.SslContextBuilder.forClient();
      setIfNotNull(keyManagerFactory, builder::keyManager);
      setIfNotNull(trustManagerFactory, builder::trustManager);
      return builder.build();
    } catch (Exception ex) {
      throw Throwables.rethrow(ex);
    }
  }

  public SslContext buildForServer() {
    Objects.requireNonNull(keyManagerFactory, "key management factory");
    try {
      io.netty.handler.ssl.SslContextBuilder builder = io.netty.handler.ssl.SslContextBuilder.forServer(keyManagerFactory);
      setIfNotNull(clientAuth, builder::clientAuth);
      setIfNotNull(trustManagerFactory, builder::trustManager);
      return builder.build();
    } catch (Exception ex) {
      throw Throwables.rethrow(ex);
    }
  }

  public SslContextBuilder clientAuth(ClientAuth val) {
    clientAuth = val;
    return this;
  }

  public SslContextBuilder keyManager(char[] keystorePasswd, String keystorePath) {
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(Files.newInputStream(Paths.get(keystorePath)), keystorePasswd);

      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(keyStore, keystorePasswd);
      return keyManager(kmf);
    } catch (Exception ex) {
      throw Throwables.rethrow(ex);
    }
  }

  public SslContextBuilder keyManager(KeyManagerFactory val) {
    keyManagerFactory = val;
    return this;
  }

  public SslContextBuilder trustManager(char[] truststorePasswd, String truststorePath) {
    try {
      KeyStore trustStore = KeyStore.getInstance("PKCS12");
      trustStore.load(Files.newInputStream(Paths.get(truststorePath)), truststorePasswd);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(trustStore);
      trustManager(tmf);
    } catch (Exception ex) {
      throw Throwables.rethrow(ex);
    }
    return this;
  }

  public SslContextBuilder trustManager(TrustManagerFactory val) {
    trustManagerFactory = val;
    return this;
  }

  private <V> void setIfNotNull(V val, Consumer<V> consumer) {
    if (val != null) {
      consumer.accept(val);
    }
  }
}
