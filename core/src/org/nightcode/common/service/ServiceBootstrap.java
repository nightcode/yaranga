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

package org.nightcode.common.service;

import java.util.function.Supplier;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.nightcode.common.base.Jvm;
import org.nightcode.common.lang.ThrowingBiConsumer;
import org.nightcode.common.logging.Log;
import org.nightcode.common.logging.Log4jLoggingHandler;
import org.nightcode.common.logging.LoggingHandler;
import org.nightcode.common.metrics.prometheus.AppInfoMetrics;
import org.nightcode.common.metrics.prometheus.ExecutorsMetrics;
import org.nightcode.common.metrics.prometheus.SessionPoolsMetrics;
import org.nightcode.common.trace.opentelemetry.TracerProviderBuilder;
import org.nightcode.common.util.ExecutorUtils;
import org.nightcode.common.util.PomUtils;
import org.nightcode.common.util.SysUtils;

/**
 * Service bootstrap.
 *
 * @param <C> the config
 */
public class ServiceBootstrap<C extends ServiceConfig> {

  static {
    SysUtils.setPropertyIfAbsent("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    SysUtils.setPropertyIfAbsent("io.prometheus.metrics.summaryQuantiles", "0.5, 0.95, 0.99");
    SysUtils.setPropertyIfAbsent("io.prometheus.metrics.summaryQuantileErrors", "0.05, 0.01, 0.001");

    Log.setLoggingHandler(
        Log4jLoggingHandler.TRACE
        , Log4jLoggingHandler.DEBUG
        , Log4jLoggingHandler.INFO
        , Log4jLoggingHandler.WARN
        , Log4jLoggingHandler.ERROR
        , Log4jLoggingHandler.FATAL
    );
  }

  private final String groupId;
  private final String artefactId;

  private volatile C            config;
  private volatile SpanExporter spanExporter;

  private volatile ThrowingBiConsumer<C, ServiceContext, Exception> serviceInitializer = (config, context) -> { };

  public ServiceBootstrap(String groupId, String artefactId) {
    this.groupId    = groupId;
    this.artefactId = artefactId;
  }

  public ServiceBootstrap<C> config(C val) {
    return config(() -> val);
  }

  public ServiceBootstrap<C> config(Supplier<C> val) {
    config = val.get();
    return this;
  }

  public ServiceBootstrap<C> initializer(ThrowingBiConsumer<C, ServiceContext, Exception> val) {
    serviceInitializer = val;
    return this;
  }

  public ServiceBootstrap<C> loggingHandler(LoggingHandler trace,
                                            LoggingHandler debug,
                                            LoggingHandler info,
                                            LoggingHandler warn,
                                            LoggingHandler error,
                                            LoggingHandler fatal) {
    Log.setLoggingHandler(trace, debug, info, warn, error, fatal);
    return this;
  }

  public ServiceBootstrap<C> spanExporter(SpanExporter val) {
    spanExporter = val;
    return this;
  }

  public void start() {
    try {
      String appVersion = PomUtils.version(groupId, artefactId);

      TracerProviderBuilder tracerProviderBuilder = TracerProviderBuilder.builder().resource(config.appName(), appVersion);
      if (spanExporter != null) {
        tracerProviderBuilder.spanExporter(spanExporter);
      }
      tracerProviderBuilder.register();

      AppInfoMetrics.builder().appName(config.appName()).appVersion(appVersion).register();
      ExecutorsMetrics.register();
      SessionPoolsMetrics.register();
      JvmMetrics.builder().register();
      ExecutorUtils.initialize(ExecutorsMetrics::addExecutor, ExecutorsMetrics::removeExecutor);

      ServiceContext context = new ServiceContextImpl();
      serviceInitializer.accept(config, context);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          context.close();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }, config.appName() + ".shutdown-hook"));

      Log.info().log(getClass(), "{} started [version: {}, bootstrap(ms): {}]", config.appName(), appVersion, Jvm.uptimeMs());
    } catch (Exception ex) {
      Log.error().log(getClass(), "cannot start " + config.appName(), ex);
      System.exit(1);
    }
  }
}
