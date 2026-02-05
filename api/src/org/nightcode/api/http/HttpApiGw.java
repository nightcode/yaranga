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

import java.util.function.Consumer;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.nightcode.api.AbstractApiGw;
import org.nightcode.api.ApiGwBuilder;

/**
 * HTTP API gateway.
 */
public final class HttpApiGw extends AbstractApiGw {

  public static HttpApiGw build(ApiGwBuilder builder) {
    return new HttpApiGw(builder);
  }

  private HttpApiGw(ApiGwBuilder builder) {
    super(builder);
  }

  @Override protected Consumer<ChannelPipeline> pipelineConsumer() {
    return p -> {
      HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
          .setMaxInitialLineLength(4096)
          .setMaxHeaderSize(8192)
          .setMaxChunkSize(8192)
          .setChunkedSupported(true)
          .setValidateHeaders(false);

      p.addLast("decoder", new HttpRequestDecoder(httpDecoderConfig));
      p.addLast("encoder", new HttpResponseEncoder());

      p.addLast("inflater", new HttpContentDecompressor(0));
      p.addLast("aggregator", new LoggableHttpObjectAggregator(context.maxBodyLengthBytes()));

      p.addLast("tx", new HttpGwTxHandler());
      p.addLast("rx", new HttpGwRxHandler(packetReader, HttpApiGw.this::consume));
    };
  }
}
