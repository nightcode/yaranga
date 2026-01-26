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

import java.util.Locale;

import org.nightcode.common.logging.Log;
import org.nightcode.common.logging.LoggingHandler;
import org.nightcode.common.props.Properties;
import org.nightcode.tracing.zipkin.SpanHandlerProvider;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;

/**
 * Logging implementation of he SpanHandler provider.
 */
public class LoggingSpanHandlerProvider implements SpanHandlerProvider {

  private static final class LoggingSpanHandler extends SpanHandler {
    private final LoggingHandler handler;

    private LoggingSpanHandler(LoggingHandler handler) {
      this.handler = handler;
    }

    @Override public boolean end(TraceContext context, MutableSpan span, Cause cause) {
      handler.log(getClass(), span::toString);
      return true;
    }
  }

  private final LoggingHandler handler;

  public LoggingSpanHandlerProvider() {
    String level = Properties.instance().getString("org.nightcode.tracing.Logging.level", "INFO").toUpperCase(Locale.US);
    switch (level) {
      case "TRACE" -> handler = Log.trace();
      case "DEBUG" -> handler = Log.debug();
      case "WARN" -> handler = Log.warn();
      case "FATAL" -> handler = Log.fatal();
      default -> handler = Log.info();
    }
  }

  @Override public SpanHandler get() {
    return new LoggingSpanHandler(handler);
  }
}
