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

import java.net.URI;
import java.util.Deque;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.nightcode.api.ApiPipe;
import org.nightcode.api.message.Request;
import org.nightcode.api.message.Response;
import org.nightcode.common.pool.Session;
import org.nightcode.common.pool.metadata.Endpoint;
import org.nightcode.net.AbstractPipe;
import org.nightcode.net.CompletablePacketContext;
import org.nightcode.net.PipeContext;

/**
 * HTTP connection.
 */
public class HttpApiPipe extends AbstractPipe<URI, Request, Response> implements ApiPipe<URI> {

  protected HttpApiPipe(PipeContext<URI, ? extends Session<URI>> context) {
    super(context);
  }

  protected HttpApiPipe(PipeContext<URI, ? extends Session<URI>> context, Deque<CompletablePacketContext<Request>> queue) {
    super(context, queue);
  }

  @Override protected boolean initPipeline(Channel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    SslContext sslContext = context.bootstrapFactory().sslContext();
    if (sslContext != null) {
      URI uri = endpoint.resolve();
      String host = uri.getHost();
      int port = (uri.getPort() != -1) ? uri.getPort() : "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
      pipeline.addLast("ssl", sslContext.newHandler(ch.alloc(), host, port));
    }
    if (loggingEnabled) {
      pipeline.addLast("logger", new LoggingHandler(context.sessionName(), logLevel));
    }

    HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
        .setMaxInitialLineLength(4096)
        .setMaxHeaderSize(8192)
        .setMaxChunkSize(8192)
        .setChunkedSupported(true)
        .setValidateHeaders(false);

    pipeline.addLast("decoder", new HttpResponseDecoder(httpDecoderConfig));
    pipeline.addLast("encoder", new HttpRequestEncoder() {
      @Override protected void sanitizeHeadersBeforeEncode(HttpRequest msg, boolean isAlwaysEmpty) {
        String acceptEncoding = msg.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
        if (acceptEncoding != null && acceptEncoding.contains("br")) {
          msg.headers().remove(HttpHeaderNames.ACCEPT_ENCODING);
          msg.headers().add(HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate");
        }
        super.sanitizeHeadersBeforeEncode(msg, isAlwaysEmpty);
      }
    });

    pipeline.addLast("inflater", new HttpContentDecompressor(0));
    pipeline.addLast("aggregator", new LoggableHttpObjectAggregator(context.maxBodyLengthBytes()));

    pipeline.addLast("tx", new HttpTxHandler(endpoint));
    pipeline.addLast("rx", new HttpRxHandler(context.packetReader(), this::consume));

    return true;
  }

  @Override protected Bootstrap remoteAddress(Bootstrap bootstrap, Endpoint<URI> en) {
    URI uri  = en.resolve();
    int port = (uri.getPort() != -1) ? uri.getPort() : "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
    return bootstrap.remoteAddress(en.resolve().getHost(), port);
  }
}
