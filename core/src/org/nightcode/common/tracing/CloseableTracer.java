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

import java.io.Closeable;
import java.util.Map;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper to make Tracer closeable.
 */
public abstract class CloseableTracer implements Tracer, Closeable {

  private final Tracer target;

  public CloseableTracer(Tracer target) {
    this.target = target;
  }

  @Override public @Nullable Span currentSpan() {
    return target.currentSpan();
  }

  @Override public Span nextSpan() {
    return target.nextSpan();
  }

  @Override public @Nullable Span nextSpan(@Nullable Span parent) {
    return target.nextSpan(parent);
  }

  @Override public SpanInScope withSpan(@Nullable Span span) {
    return target.withSpan(span);
  }

  @Override public ScopedSpan startScopedSpan(String name) {
    return target.startScopedSpan(name);
  }

  @Override public Span.Builder spanBuilder() {
    return target.spanBuilder();
  }

  @Override public TraceContext.Builder traceContextBuilder() {
    return target.traceContextBuilder();
  }

  @Override public CurrentTraceContext currentTraceContext() {
    return target.currentTraceContext();
  }

  @Override public @Nullable SpanCustomizer currentSpanCustomizer() {
    return target.currentSpanCustomizer();
  }

  @Override public Map<String, String> getAllBaggage() {
    return target.getAllBaggage();
  }

  @Override public Baggage getBaggage(String name) {
    return target.getBaggage(name);
  }

  @Override public @Nullable Baggage getBaggage(TraceContext traceContext, String name) {
    return target.getBaggage(traceContext, name);
  }

  @Override public Baggage createBaggage(String name) {
    return target.createBaggage(name);
  }

  @Override public Baggage createBaggage(String name, String value) {
    return target.createBaggage(name, value);
  }
}
