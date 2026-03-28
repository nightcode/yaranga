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

package org.nightcode.common.logging.opentelemetry;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.nightcode.common.props.Properties;

import static java.util.Collections.emptyMap;

/**
 * Implementation of {@link ContextDataProvider} for adding OpenTelemetry trace context to log events.
 */
public class OtelLog4jContextDataProvider implements ContextDataProvider {

  private static final String PREFIX = "org.nightcode.trace.opentelemetry";

  public static final String TRACE_ID_KEY    = Properties.instance().getString(PREFIX + ".traceIdKey", "trace_id");
  public static final String SPAN_ID_KEY     = Properties.instance().getString(PREFIX + ".spanIdKey", "span_id");
  public static final String TRACE_FLAGS_KEY = Properties.instance().getString(PREFIX + ".traceFlagsKey", "trace_flags");

  @Override public Map<String, String> supplyContextData() {
    Context context = Context.current();
    Span    span    = Span.fromContext(context);
    if (!span.getSpanContext().isValid() || ThreadContext.containsKey(TRACE_ID_KEY)) {
      return emptyMap();
    }

    SpanContext spanContext = span.getSpanContext();

    Map<String, String> contextData = new HashMap<>();
    contextData.put(TRACE_ID_KEY,    spanContext.getTraceId());
    contextData.put(SPAN_ID_KEY,     spanContext.getSpanId());
    contextData.put(TRACE_FLAGS_KEY, spanContext.getTraceFlags().asHex());

    return contextData;
  }
}
