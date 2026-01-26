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

package org.nightcode.tracing.zipkin.handler;

import org.nightcode.common.props.Properties;
import org.nightcode.tracing.zipkin.SpanHandlerProvider;

import brave.handler.SpanHandler;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * Zipkin implementation of the SpanHandler provider.
 */
public class ZipkinSpanHandlerProvider implements SpanHandlerProvider {

  private static final String DEF_ADDRESS = "http://127.0.0.1:9411/api/v2/spans";

  private static final String ADDRESS = Properties.instance().getString("org.nightcode.tracing.zipkin.Address", DEF_ADDRESS);

  @Override public SpanHandler get() {
    BytesMessageSender sender = URLConnectionSender.create(ADDRESS);
    return AsyncZipkinSpanHandler.create(sender);
  }
}
