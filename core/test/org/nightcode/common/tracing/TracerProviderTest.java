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

package org.nightcode.common.tracing;

import java.io.IOException;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import org.nightcode.common.props.Properties;
import org.nightcode.common.props.SystemPropertiesStorage;

import org.junit.Assert;
import org.junit.Test;

import static org.nightcode.common.tracing.NoopTracerProvider.NOOP;

/**
 * Unit tests for {@link TracerProvider}.
 */
public class TracerProviderTest {

  @Test public void tracer() {
    try (CloseableTracer tracer = TracerProvider.instance()) {
      Assert.assertEquals(NOOP, tracer);
      
      Assert.assertEquals(Span.NOOP, tracer.currentSpan());
      Assert.assertEquals(Span.NOOP, tracer.nextSpan());
      Assert.assertEquals(Span.NOOP, tracer.nextSpan(Span.NOOP));
      Assert.assertEquals(ScopedSpan.NOOP, tracer.startScopedSpan("test"));
      Assert.assertEquals(Span.Builder.NOOP, tracer.spanBuilder());
      Assert.assertEquals(TraceContext.Builder.NOOP, tracer.traceContextBuilder());
      Assert.assertEquals(CurrentTraceContext.NOOP, tracer.currentTraceContext());
      Assert.assertEquals(SpanCustomizer.NOOP, tracer.currentSpanCustomizer());
      Assert.assertEquals(BaggageManager.NOOP.getAllBaggage(), tracer.getAllBaggage());
      Assert.assertEquals(Baggage.NOOP, tracer.getBaggage("test"));
      Assert.assertEquals(Baggage.NOOP, tracer.getBaggage(TraceContext.NOOP, "test"));
      Assert.assertEquals(Baggage.NOOP, tracer.createBaggage("test"));
      Assert.assertEquals(Baggage.NOOP, tracer.createBaggage("test", "test"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test public void noop() throws IOException {
    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty("org.nightcode.tracing.TracerProvider", "FakeTracerProvider");
    try (CloseableTracer tracer = TracerProvider.instance()) {
      Assert.assertEquals(NOOP, tracer);
    }
  }
}
