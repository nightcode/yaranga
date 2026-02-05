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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.nightcode.common.logging.Log;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;

/**
 * Loggable HTTP object aggregator.
 */
class LoggableHttpObjectAggregator extends HttpObjectAggregator {

  private static final FullHttpResponse TOO_LARGE
      = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, REQUEST_ENTITY_TOO_LARGE, EMPTY_BUFFER);

  private static final FullHttpResponse TOO_LARGE_CLOSE
      = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, REQUEST_ENTITY_TOO_LARGE, EMPTY_BUFFER);

  LoggableHttpObjectAggregator(int maxContentLength) {
    super(maxContentLength);
  }

  LoggableHttpObjectAggregator(int maxContentLength, boolean closeOnExpectationFailed) {
    super(maxContentLength, closeOnExpectationFailed);
  }

  @Override protected void handleOversizedMessage(ChannelHandlerContext ctx, HttpMessage oversized) {
    if (oversized instanceof HttpRequest) {
      Log.info().log(getClass(), "[{}] request entity too large", ctx.channel());
      // send back a 413 and close the connection

      // If the client started to send data already, close because it's impossible to recover.
      // If keep-alive is off and 'Expect: 100-continue' is missing, no need to leave the connection open.
      if (oversized instanceof FullHttpMessage || !HttpUtil.is100ContinueExpected(oversized) && !HttpUtil.isKeepAlive(oversized)) {
        ChannelFuture future = ctx.writeAndFlush(TOO_LARGE_CLOSE.retainedDuplicate());
        future.addListener((ChannelFutureListener) f -> {
          if (!f.isSuccess()) {
            Log.info().log(getClass(), "Failed to send a 413 Request Entity Too Large.", f.cause());
          }
          ctx.close();
        });
      } else {
        ctx.writeAndFlush(TOO_LARGE.retainedDuplicate()).addListener((ChannelFutureListener) f -> {
          if (!f.isSuccess()) {
            Log.info().log(getClass(), "Failed to send a 413 Request Entity Too Large.", f.cause());
            ctx.close();
          }
        });
      }
    } else if (oversized instanceof HttpResponse) {
      Log.info().log(getClass(), "[{}] response entity too large", ctx.channel());
      ctx.close();
      throw new TooLongFrameException("Response entity too large: " + oversized);
    } else {
      throw new IllegalStateException();
    }
  }
}
