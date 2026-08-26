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

package org.nightcode.net.impl;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.nightcode.common.pool.Session;
import org.nightcode.common.pool.SessionPool;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.common.pool.metadata.InetSocketAddressEndpoint;
import org.nightcode.common.util.Clock;
import org.nightcode.common.util.Closeables;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.net.BootstrapFactory;
import org.nightcode.net.PacketContext;
import org.nightcode.net.PacketReader;
import org.nightcode.net.PacketRxHandler;
import org.nightcode.net.PacketTxHandler;
import org.nightcode.net.PacketWriter;
import org.nightcode.net.Pipe;
import org.nightcode.net.PipeContext;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for {@link TcpIpPipe}.
 */
public class TcpIpPipeTest {

  public static final class TestInitializer extends ChannelInitializer<Channel> {

    private final Consumer<ChannelPipeline> pipelineConsumer;

    public TestInitializer(Consumer<ChannelPipeline> pipelineConsumer) {
      this.pipelineConsumer = pipelineConsumer;
    }

    @Override protected void initChannel(Channel channel) {
      ChannelPipeline pipeline = channel.pipeline();
      pipelineConsumer.accept(pipeline);
    }
  }

  public static final class TestPacketWriter implements PacketWriter<byte[]> {
    @Override public int size(byte[] packet) {
      return packet.length;
    }

    @Override public void write(OutputStream out, byte[] packet) throws IOException {
      out.write(packet);
    }
  }

  public static final class TestPacketReader implements PacketReader<byte[]> {
    @Override public byte[] read(byte[] buffer, int offset, int length) {
      return Arrays.copyOfRange(buffer, offset, offset + length);
    }
  }

  public static final class KeyHolder {
    private PrivateKey      privateKey;
    private PublicKey       publicKey;
    private X509Certificate certificate;
  }

  public static final class TcpIpTimeoutCapturingPipe<Q, R> extends TcpIpPipe<Q, R> {

    private volatile EventLoop eventLoop;

    public TcpIpTimeoutCapturingPipe(PipeContext<InetSocketAddress, ? extends Session<InetSocketAddress>> context) {
      super(context);
    }

    @Override protected void destroyImpl() {
      Closeables.close(eventLoop);
      super.destroyImpl();
    }

    @Override protected ChannelFuture openChannel() {
      eventLoop = new SingleThreadIoEventLoop(null, ExecutorUtils.namedThreadFactory("TcpIpTimeoutCapturingPipe"), LocalIoHandler.newFactory());
      LocalChannel channel = new LocalChannel();
      eventLoop.register(channel);
      DefaultChannelPromise channelFuture = new DefaultChannelPromise(channel);

      channelFuture.setFailure(new ConnectTimeoutException("Connection timeout"));

      return channelFuture;
    }
  }

  private final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

  public TcpIpPipeTest() throws CertificateException {
    // do nothing
  }

  @Test public void testConnect() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();

    final CountDownLatch negotiateLatch = new CountDownLatch(1);


    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<>() {
            @Override protected void initChannel(Channel ch) {
              // do nothing
            }
          });

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<Object, Object>> context = createPipeContext(endpoint);

      pipe = new TcpIpPipe<>(context);
      pipe.addListener(event -> {
        if (event.type().equals(Session.State.ACTIVE)) {
          negotiateLatch.countDown();
        }
      });
      pipe.initialize();

      assertTrue(negotiateLatch.await(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testConnectTimeout() throws Exception {
    String                      address  = "10.255.255.1:" + 80;
    Endpoint<InetSocketAddress> endpoint = new InetSocketAddressEndpoint(address);

    PipeContext<InetSocketAddress, TcpIpPipe<Object, Object>> context = createPipeContext(endpoint);

    final CompletableFuture<Session.State> state = new CompletableFuture<>();

    TcpIpPipe<Object, Object> pipe = new TcpIpTimeoutCapturingPipe<>(context);
    pipe.addListener(event -> {
      if (Session.State.CREATING.equals(event.type())) {
        return;
      }
      state.complete(event.type());
    });

    Session.State actual;
    try {
      pipe.initialize();
      actual = state.get(1_000, TimeUnit.MILLISECONDS);
    } finally {
      pipe.destroy();
    }

    assertEquals(Session.State.TIMEOUT, actual);
  }

  @Test public void testSendAsync() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    final CountDownLatch sendLatch = new CountDownLatch(1);

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      PacketRxHandler<byte[]> handler = new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
        assertArrayEquals(request, context.packet());
        sendLatch.countDown();
      });
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> p.addLast("in", handler)));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter());
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      pipe.sendAsync(request);

      assertTrue(sendLatch.await(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testSendAsyncTimeout() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      PacketRxHandler<byte[]> handler = new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> assertArrayEquals(request, context.packet()));
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> p.addLast("in", handler)));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);


      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter());
      pipe = new TcpIpPipe<>(context);
      CompletableFuture<Void> cf = pipe.sendAsync(request);

      Thread.sleep(SECONDS.toMillis(5));
      pipe.initialize();

      try {
        cf.get(1, SECONDS);
        fail("MUST throw ConnectionException");
      } catch (Exception ex) {
        assertTrue(ex.getMessage().contains(" expired, channel"));
      }
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testSendAsyncReceive() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in"
                , new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
                  assertArrayEquals(request, context.packet());
                  PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response);
                  p.channel().writeAndFlush(responseContext);
                }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter());
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      assertArrayEquals(response, rf.get(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testSendAsyncReceiveTimeout() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in"
                , new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
                  assertArrayEquals(request, context.packet());
                  PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                      , Clock.sys().nanoTime() + SECONDS.toNanos(5));
                  p.channel().writeAndFlush(responseContext);
                }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter());
      pipe = new TcpIpPipe<>(context);
      CompletableFuture<byte[]> cf = pipe.sendReceiveAsync(request);

      Thread.sleep(SECONDS.toMillis(5));
      pipe.initialize();

      try {
        cf.get(1, SECONDS);
        fail("MUST throw ConnectionException");
      } catch (Exception ex) {
        assertTrue(ex.getMessage().contains(" expired, channel"));
      }
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test void testDestroyPipeCompletesWithEof() {
    String address  = "10.255.255.1:" + 80;
    var    endpoint = new InetSocketAddressEndpoint(address);
    var    context  = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter());

    TcpIpPipe<byte[], byte[]> pipe    = new TcpIpPipe<>(context);
    CompletableFuture<byte[]> pending = pipe.sendReceiveAsync(new byte[]{0x01});

    assertFalse(pending.isDone(), "response future must be in progress");

    pipe.destroy();

    assertTrue(pending.isCompletedExceptionally(), "response future must complete exceptionally");
    ExecutionException ex = assertThrows(ExecutionException.class, () -> pending.get(1, TimeUnit.SECONDS));
    assertSame(Pipe.EOF, ex.getCause());
  }

  @Test public void testServerSsl() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder  root             = createCertificate("serverCA", null);
    SslContext serverSslContext = SslContextBuilder.forServer(root.privateKey, root.certificate).build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      SslContext clientSslContext = SslContextBuilder.forClient().trustManager(root.certificate).build();

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      assertArrayEquals(response, rf.get(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testServerSslException() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder  root             = createCertificate("serverCA", null);
    SslContext serverSslContext = SslContextBuilder.forServer(root.privateKey, root.certificate).build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      KeyHolder  root2            = createCertificate("clientCA", null);
      SslContext clientSslContext = SslContextBuilder.forClient().trustManager(root2.certificate).build();

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context
          = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      ExecutionException ex = assertThrows(ExecutionException.class, () -> rf.get(5, SECONDS));
      assertInstanceOf(SSLHandshakeException.class, ex.getCause());
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testServerSslChain() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder  root             = createCertificate("serverCA", null);
    KeyHolder  signed           = createCertificate("server", root);
    SslContext serverSslContext = SslContextBuilder.forServer(signed.privateKey, signed.certificate, root.certificate).build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      SslContext clientSslContext = SslContextBuilder.forClient().trustManager(signed.certificate).build();

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context
          = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      assertArrayEquals(response, rf.get(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testClientSsl() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder rootClient   = createCertificate("clientCA", null);
    KeyHolder signedClient = createCertificate("client", rootClient);

    SslContext serverSslContext = SslContextBuilder
        .forClient()
        .endpointIdentificationAlgorithm(null)
        .trustManager(signedClient.certificate)
        .build();

    SslContext clientSslContext = SslContextBuilder
        .forServer(signedClient.privateKey, signedClient.certificate, rootClient.certificate)
        .build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context
          = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      assertArrayEquals(response, rf.get(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testClientSslException() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder rootClient   = createCertificate("clientCA", null);
    KeyHolder signedClient = createCertificate("client", rootClient);

    KeyHolder rootOtherClient   = createCertificate("otherClientCA", null);
    KeyHolder signedOtherClient = createCertificate("otherClient", rootOtherClient);

    SslContext serverSslContext = SslContextBuilder
        .forClient()
        .trustManager(signedOtherClient.certificate)
        .build();

    SslContext clientSslContext = SslContextBuilder
        .forServer(signedClient.privateKey, signedClient.certificate, rootClient.certificate)
        .build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context
          = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      ExecutionException ex = assertThrows(ExecutionException.class, () -> rf.get(5, SECONDS));
      assertInstanceOf(SSLException.class, ex.getCause());
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testMutualSsl() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder rootServer   = createCertificate("serverCA", null);
    KeyHolder signedServer = createCertificate("server", rootServer);

    KeyHolder rootClient   = createCertificate("clientCA", null);
    KeyHolder signedClient = createCertificate("client", rootClient);

    SslContext serverSslContext = SslContextBuilder
        .forServer(signedServer.privateKey, signedServer.certificate, rootServer.certificate)
        .clientAuth(ClientAuth.REQUIRE)
        .trustManager(signedClient.certificate, rootClient.certificate)
        .build();

    SslContext clientSslContext = SslContextBuilder
        .forClient()
        .keyManager(signedClient.privateKey, signedClient.certificate, rootClient.certificate)
        .trustManager(signedServer.certificate)
        .build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context
          = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      assertArrayEquals(response, rf.get(5, SECONDS));
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testMutualSslException() throws Exception {
    TcpIpPipe<byte[], byte[]> pipe            = null;
    ServerBootstrap           serverBootstrap = new ServerBootstrap();
    Random                    random          = new Random(0);

    byte[] request = new byte[24];
    random.nextBytes(request);

    byte[] response = new byte[48];
    random.nextBytes(response);

    KeyHolder rootServer   = createCertificate("serverCA", null);
    KeyHolder signedServer = createCertificate("server", rootServer);

    KeyHolder rootClient   = createCertificate("clientCA", null);
    KeyHolder signedClient = createCertificate("client", rootClient);

    KeyHolder rootOtherClient   = createCertificate("otherClientCA", null);
    KeyHolder signedOtherClient = createCertificate("otherClient", rootOtherClient);

    SslContext serverSslContext = SslContextBuilder
        .forServer(signedServer.privateKey, signedServer.certificate, rootServer.certificate)
        .clientAuth(ClientAuth.REQUIRE)
        .trustManager(signedClient.certificate, rootClient.certificate)
        .protocols("TLSv1.2")
        .build();

    SslContext clientSslContext = SslContextBuilder
        .forClient()
        .keyManager(signedOtherClient.privateKey, signedOtherClient.certificate, rootOtherClient.certificate)
        .trustManager(signedServer.certificate)
        .build();

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new TestInitializer(p -> {
            p.addLast("ssl", serverSslContext.newHandler(p.channel().alloc()));
            p.addLast("out", new PacketTxHandler<>());
            p.addLast("in", new PacketRxHandler<>(new TestPacketReader(), (ctx, context) -> {
              PacketContext<byte[]> responseContext = new PacketContextImpl<>(new TestPacketWriter(), context.packetId(), response
                  , Clock.sys().nanoTime() + SECONDS.toNanos(5));
              p.channel().writeAndFlush(responseContext);
            }));
          }));

      Channel                     serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      InetSocketAddress           address       = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint      = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<byte[], byte[]>> context
          = createPipeContext(endpoint, new TestPacketReader(), new TestPacketWriter(), clientSslContext);
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();
      CompletableFuture<byte[]> rf = pipe.sendReceiveAsync(request);

      ExecutionException ex = assertThrows(ExecutionException.class, () -> rf.get(5, SECONDS));
      assertInstanceOf(SSLException.class, ex.getCause());
    } finally {
      if (pipe != null) {
        pipe.destroy();
      }
    }
  }

  @Test public void testPacketTimeout() throws Exception {
    TcpIpPipe<Any, Any> pipe;
    InetSocketAddress   address;
    ServerBootstrap     serverBootstrap = new ServerBootstrap();
    ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<>() {
      @Override protected void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("out", new PacketTxHandler<>());
        p.addLast("int", new PacketRxHandler<>(new ProtobufPacketReader<>(Any.getDefaultInstance()), (ctx, mh) -> {
          PacketContext<Any> responseContext = new PacketContextImpl<>(new ProtobufPacketWriter<>(), mh.packetId(), mh.packet());
          p.channel().writeAndFlush(responseContext);
        }));
      }
    };

    try (EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory())) {
      serverBootstrap.group(eventLoopGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(channelInitializer);

      Channel serverChannel = serverBootstrap.bind(new InetSocketAddress(0)).sync().channel();
      address = (InetSocketAddress) serverChannel.localAddress();
      Endpoint<InetSocketAddress> endpoint = new InetSocketAddressEndpoint(address);

      PipeContext<InetSocketAddress, TcpIpPipe<Any, Any>> context = createPipeContext(endpoint, new ProtobufPacketReader<>(Any.getDefaultInstance()), new ProtobufPacketWriter<>());
      pipe = new TcpIpPipe<>(context);
      pipe.initialize();

      Any request  = Any.pack(StringValue.newBuilder().setValue(UUID.randomUUID().toString()).build());
      Any response = pipe.sendReceiveAsync(request).get(5, TimeUnit.SECONDS);
      assertEquals(request, response);
    }

    try {
      assertEquals(0, pipe.queueSize());

      Any                    request = Any.pack(StringValue.newBuilder().setValue(UUID.randomUUID().toString()).build());
      CompletableFuture<Any> cf      = pipe.sendReceiveAsync(request);
      assertEquals(1, pipe.queueSize());
      cf.completeExceptionally(new IOException("timeout"));
      assertEquals(0, pipe.queueSize());

      try {
        cf.get();
        fail("should throw ExecutionException");
      } catch (Exception ex) {
        assertEquals("java.io.IOException: timeout", ex.getMessage());
      }
    } finally {
      pipe.destroy();
    }
  }

  private KeyHolder createCertificate(String name, KeyHolder rootCa) throws Exception {
    KeyPairGenerator       keyPairGenerator       = KeyPairGenerator.getInstance("RSA");
    AlgorithmParameterSpec algorithmParameterSpec = new RSAKeyGenParameterSpec(256 * 8, BigInteger.valueOf(65537));
    keyPairGenerator.initialize(algorithmParameterSpec);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    PublicKey  publicKey  = keyPair.getPublic();
    PrivateKey privateKey = keyPair.getPrivate();

    BigInteger serial    = BigInteger.valueOf(System.currentTimeMillis());
    Date       notBefore = new Date();
    Date       notAfter  = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));
    X500Name   subject   = new X500Name("CN=" + name + ",OU=Station,O=South Pole Station,L=South Pole,ST=Antarctica,C=AQ");

    X500Name   issuer;
    PublicKey  signerPublicKey;
    PrivateKey signerPrivateKey;

    if (rootCa != null) {
      signerPublicKey  = rootCa.publicKey;
      signerPrivateKey = rootCa.privateKey;

      X509Certificate caCertificate = rootCa.certificate;
      issuer = X500Name.getInstance(caCertificate.getSubjectX500Principal().getEncoded());
    } else {
      signerPublicKey  = publicKey;
      signerPrivateKey = privateKey;

      issuer = subject;
    }

    X509v3CertificateBuilder certificateBuilder
        = new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKey);
    addExtensions(rootCa != null, 3, Collections.emptyList(), certificateBuilder, publicKey, signerPublicKey);

    byte[] certificateBytes = certificateBuilder.build(contentSigner(signerPrivateKey)).getEncoded();

    X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));

    KeyHolder keyHolder = new KeyHolder();
    keyHolder.privateKey  = privateKey;
    keyHolder.publicKey   = publicKey;
    keyHolder.certificate = certificate;

    return keyHolder;
  }

  private void addExtensions(boolean caCert, int pathLenConstraint, List<KeyUsage> keyUsages, X509v3CertificateBuilder builder,
                             PublicKey publicKey, PublicKey signerPublicKey) throws GeneralSecurityException {
    KeyUsage         keyUsage;
    BasicConstraints basicConstraints;

    if (caCert) {
      keyUsage         = caKeyUsage(keyUsages);
      basicConstraints = (pathLenConstraint < 0) ? new BasicConstraints(true) : new BasicConstraints(pathLenConstraint);
    } else {
      keyUsage         = nonCaKeyUsage(keyUsages);
      basicConstraints = new BasicConstraints(false);
    }

    JcaX509ExtensionUtils  x509ExtensionUtils     = new JcaX509ExtensionUtils();
    AuthorityKeyIdentifier authorityKeyIdentifier = x509ExtensionUtils.createAuthorityKeyIdentifier(signerPublicKey);
    SubjectKeyIdentifier   subjectKeyIdentifier   = x509ExtensionUtils.createSubjectKeyIdentifier(publicKey);

    try {
      builder.addExtension(Extension.keyUsage, true, keyUsage);
      builder.addExtension(Extension.basicConstraints, true, basicConstraints);
      builder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
      builder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
      builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[]{new GeneralName(GeneralName.iPAddress, "0:0:0:0:0:0:0:0")}));
    } catch (CertIOException ex) {
      throw new GeneralSecurityException(ex);
    }
  }

  private ContentSigner contentSigner(PrivateKey privateKey) throws GeneralSecurityException {
    try {
      return new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
    } catch (OperatorCreationException ex) {
      throw new GeneralSecurityException(ex);
    }
  }

  private KeyUsage caKeyUsage(List<KeyUsage> keyUsages) {
    if (keyUsages.isEmpty()) {
      return new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign);
    }
    return getKeyUsage(keyUsages);
  }

  private KeyUsage nonCaKeyUsage(List<KeyUsage> keyUsages) {
    if (keyUsages.isEmpty()) {
      return new KeyUsage(KeyUsage.nonRepudiation | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment);
    }
    return getKeyUsage(keyUsages);
  }

  private org.bouncycastle.asn1.x509.KeyUsage getKeyUsage(List<KeyUsage> keyUsage) {
    int usage = 0;
    for (KeyUsage ku : keyUsage) {
      usage |= ku.getPadBits();
    }
    return new org.bouncycastle.asn1.x509.KeyUsage(usage);
  }

  private <Q, R> PipeContext<InetSocketAddress, TcpIpPipe<Q, R>> createPipeContext(Endpoint<InetSocketAddress> endpoint) {
    return createPipeContext(endpoint, (buffer, offset, length) -> null, new PacketWriter<>() {
      @Override public int size(Q packet) {
        return 0;
      }

      @Override public void write(OutputStream out, Q packet) {

      }
    });
  }

  private <Q, R> PipeContext<InetSocketAddress, TcpIpPipe<Q, R>> createPipeContext(Endpoint<InetSocketAddress> endpoint,
                                                                                   PacketReader<R> packetReader,
                                                                                   PacketWriter<Q> packetWriter) {
    return createPipeContext(endpoint, packetReader, packetWriter, null);
  }

  private <Q, R> PipeContext<InetSocketAddress, TcpIpPipe<Q, R>> createPipeContext(Endpoint<InetSocketAddress> endpoint,
                                                                                   PacketReader<R> packetReader,
                                                                                   PacketWriter<Q> packetWriter,
                                                                                   SslContext sslContext) {
    return new PipeContext<>() {
      @Override public BootstrapFactory bootstrapFactory() {
        return BootstrapFactory.tcpIpFactory().withSsl(sslContext);
      }

      @Override public <M> PacketReader<M> packetReader() {
        // noinspection unchecked
        return (PacketReader<M>) packetReader;
      }

      @Override public <M> PacketWriter<M> packetWriter() {
        // noinspection unchecked
        return (PacketWriter<M>) packetWriter;
      }

      @Override public int nThreads() {
        return Runtime.getRuntime().availableProcessors();
      }

      @Override public Endpoint<InetSocketAddress> endpoint() {
        return endpoint;
      }

      @Override public TcpIpPipe<Q, R> session() {
        return null;
      }

      @Override public String sessionName() {
        return "localConnection";
      }

      @Override public long createTimeoutMs() {
        return 200L;
      }

      @Override public long executeTimeoutNs() {
        return 500L;
      }

      @Override public SessionPool<InetSocketAddress, TcpIpPipe<Q, R>> pool() {
        return null;
      }

      @Override public String poolName() {
        return "localPool";
      }

      @Override public long queueTimeoutNs() {
        return SECONDS.toNanos(5);
      }

      @Override public long rebuildTimeoutMs() {
        return 500L;
      }
    };
  }
}
