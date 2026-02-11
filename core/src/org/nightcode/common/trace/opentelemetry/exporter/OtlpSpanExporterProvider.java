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

package org.nightcode.common.trace.opentelemetry.exporter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.nightcode.common.props.Properties;
import org.nightcode.common.trace.opentelemetry.SpanExporterProvider;

/**
 * OLTP SpanExporter provider.
 */
public class OtlpSpanExporterProvider implements SpanExporterProvider {

  private static final String DEF_PROTOCOL   = "HTTP";
  private static final String DEF_ENDPOINT   = "http://127.0.0.1:4318/v1/traces";
  private static final String HEADER_API_KEY = "api-key";

  private final long   timeoutMs = Properties.instance().getLong("org.nightcode.trace.exporter.OtlpSpanExporter.timeoutMs", 10_000);
  private final String protocol  = Properties.instance().getString("org.nightcode.trace.exporter.OtlpSpanExporter.protocol", DEF_PROTOCOL);
  private final String endpoint  = Properties.instance().getString("org.nightcode.trace.exporter.OtlpSpanExporter.endpoint", DEF_ENDPOINT);
  private final String apiKey    = Properties.instance().getString("org.nightcode.trace.exporter.OtlpSpanExporter.staticApiKey", null);

  @Override public SpanExporter get() {
    return switch (protocol) {
      case "GRPC" -> OtlpGrpcSpanExporter.builder()
          .setEndpoint(endpoint)
          .setHeaders(headersSupplier())
          .setTimeout(timeoutMs, TimeUnit.MILLISECONDS)
          .build();
      case "HTTP" -> OtlpHttpSpanExporter.builder()
          .setEndpoint(endpoint)
          .setHeaders(headersSupplier())
          .setTimeout(timeoutMs, TimeUnit.MILLISECONDS)
          .build();
      default -> throw new IllegalStateException("Unexpected PROTOCOL value: " + protocol);
    };
  }
  private Supplier<Map<String, String>> headersSupplier() {
    return () -> {
      if (apiKey == null || apiKey.isEmpty()) {
        return Collections.emptyMap();
      }
      return Map.of(HEADER_API_KEY, apiKey);
    };
  }
}
