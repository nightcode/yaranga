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

package org.nightcode.tracing.zipkin;

import java.util.Objects;
import java.util.ServiceLoader;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.nightcode.common.base.Jvm;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;
import org.nightcode.common.tracing.CloseableTracer;
import org.nightcode.common.tracing.TracerProvider;
import org.nightcode.common.util.Closeables;
import org.nightcode.tracing.zipkin.handler.LoggingSpanHandlerProvider;

import brave.Tracing;
import brave.baggage.BaggagePropagation;
import brave.context.log4j2.ThreadContextScopeDecorator;
import brave.handler.SpanHandler;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;

/**
 * Brave based implementation of TracerProvider.
 */
public final class BraveTracerProvider implements TracerProvider {

  private final String                                               localServiceName;
  private final SpanHandler                                          spanHandler;
  private final Sampler                                              sampler;
  private final brave.propagation.CurrentTraceContext.ScopeDecorator scopeDecorator;
  private final Propagation.Factory                                  propagationFactory;

  private final ServiceLoader<SpanHandlerProvider> serviceLoader = ServiceLoader.load(SpanHandlerProvider.class);

  public BraveTracerProvider() {
    float probability = Float.parseFloat(Properties.instance().getString("org.nightcode.tracing.SamplingRatio", "0.01f"));
    String spanHandlerClassName = Properties.instance().getString("org.nightcode.tracing.zipkin.SpanHandlerProvider"
        , LoggingSpanHandlerProvider.class.getName());

    localServiceName   = Properties.instance().getString("org.nightcode.tracing.LocalServiceName", "pid:" + Jvm.pid());
    scopeDecorator     = ThreadContextScopeDecorator.get();
    sampler            = Sampler.create(probability);
    spanHandler        = spanHandler(spanHandlerClassName);
    propagationFactory = BaggagePropagation.newFactoryBuilder(B3Propagation.FACTORY).build();
  }

  @Override public CloseableTracer get() {
    ThreadLocalCurrentTraceContext braveCurrentTraceContext
        = ThreadLocalCurrentTraceContext.newBuilder().addScopeDecorator(scopeDecorator).build();

    Tracing tracing = Tracing.newBuilder()
        .currentTraceContext(braveCurrentTraceContext)
        .supportsJoin(false)
        .traceId128Bit(true)
        .propagationFactory(propagationFactory)
        .localServiceName(localServiceName)
        .sampler(sampler)
        .addSpanHandler(spanHandler)
        .build();

    brave.Tracer        braveTracer   = tracing.tracer();
    CurrentTraceContext bridgeContext = new BraveCurrentTraceContext(braveCurrentTraceContext);

    return new CloseableTracer(new BraveTracer(braveTracer, bridgeContext)) {
      @Override public void close() {
        Closeables.close(tracing);
      }
    };
  }

  private SpanHandler spanHandler(String providerClass) {
    Objects.requireNonNull(providerClass, "SpanHandlerProvider class name must not be null");
    for (SpanHandlerProvider provider : serviceLoader) {
      if (providerClass.equals(provider.getClass().getName())) {
        SpanHandler spanHandler = provider.get();
        Log.info().log(getClass(), "initialized SpanHandler: {}", spanHandler.getClass().getName());
        return spanHandler;
      }
    }
    Log.warn().log(getClass(), "unable to find SpanHandlerProvider {}, fallback to SpanHandler.NOOP", providerClass);
    return SpanHandler.NOOP;
  }
}
