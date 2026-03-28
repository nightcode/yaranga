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

package org.nightcode.common.trace.opentelemetry;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.nightcode.common.logging.Log;
import org.nightcode.common.logging.LoggingHandler;
import org.nightcode.common.props.Properties;

/**
 * Logging SpanExporter implementation.
 */
public class LoggingSpanExporter implements SpanExporter {

  private static final ThreadLocal<StringBuilder> STRING_BUILDER_TH = ThreadLocal.withInitial(() -> new StringBuilder(128));

  private final LoggingHandler handler;

  private final AtomicBoolean isShutdown = new AtomicBoolean();

  public LoggingSpanExporter() {
    String level = Properties.instance().getString("org.nightcode.trace.exporter.LoggingSpanExporter.level", "INFO").toUpperCase(Locale.US);
    switch (level) {
      case "TRACE" -> handler = Log.trace();
      case "DEBUG" -> handler = Log.debug();
      case "WARN" -> handler = Log.warn();
      case "FATAL" -> handler = Log.fatal();
      default -> handler = Log.info();
    }
  }

  public LoggingSpanExporter(LoggingHandler handler) {
    this.handler = handler;
  }

  @Override public CompletableResultCode export(Collection<SpanData> spans) {
    if (isShutdown.get()) {
      return CompletableResultCode.ofFailure();
    }

    StringBuilder sb = STRING_BUILDER_TH.get();
    for (SpanData span : spans) {
      sb.setLength(0);
      InstrumentationScopeInfo instrumentationScopeInfo = span.getInstrumentationScopeInfo();
      sb.append("'")
          .append(span.getName())
          .append("' : ")
          .append(span.getTraceId())
          .append(" ")
          .append(span.getSpanId()).append(" ")
          .append(span.getKind())
          .append(" [tracer: ")
          .append(instrumentationScopeInfo.getName())
          .append(":")
          .append(instrumentationScopeInfo.getVersion() == null ? "" : instrumentationScopeInfo.getVersion())
          .append("] ")
          .append(span.getAttributes());
      handler.log(getClass(), sb.toString());
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      handler.log(getClass(), "calling shutdown() multiple times");
      return CompletableResultCode.ofSuccess();
    }
    return flush();
  }
}
