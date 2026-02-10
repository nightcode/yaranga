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

import java.util.concurrent.TimeUnit;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.nightcode.common.props.Properties;
import org.nightcode.common.trace.opentelemetry.SpanExporterProvider;

/**
 * OLTP SpanExporter provider.
 */
public class OtlpSpanExporterProvider implements SpanExporterProvider {

  private final long   timeoutMs = Properties.instance().getLong("org.nightcode.trace.exporter.OtlpSpanExporter.timeoutMs", 10_000);
  private final String protocol  = Properties.instance().getString("org.nightcode.trace.exporter.OtlpSpanExporter.protocol", "HTTP");
  private final String endpoint  = Properties.instance().getString("org.nightcode.trace.exporter.OtlpSpanExporter.endpoint"
      , "http://127.0.0.1:4318/v1/traces");

  @Override public SpanExporter get() {
    return switch (protocol) {
      case "GRPC" -> OtlpGrpcSpanExporter.builder()
          .setEndpoint(endpoint)
          .addHeader("api-key", "value")
          .setTimeout(timeoutMs, TimeUnit.MILLISECONDS)
          .build();
      case "HTTP" -> OtlpHttpSpanExporter.builder()
          .setEndpoint(endpoint)
          .addHeader("api-key", "value")
          .setTimeout(timeoutMs, TimeUnit.MILLISECONDS)
          .build();
      default -> throw new IllegalStateException("Unexpected PROTOCOL value: " + protocol);
    };
  }
}
