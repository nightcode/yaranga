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

package org.nightcode.common.monitoring;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

@Beta
public final class MonitoringManager {

  private static final MonitoringManager INSTANCE;

  private static final List<Quantile> DEF_QUANTILES = asList(
        new Quantile(0.5, 0.05)
      , new Quantile(0.95, 0.01)
      , new Quantile(0.99, 0.001));

  static {
    MonitoringEngine engine = null;
    String cname = null;
    try {
      cname = Properties.instance().getString("monitoring.engine", NoopMonitoringEngine.class.getName());
      if (cname != null) {
        try {
          Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(cname);
          engine = (MonitoringEngine) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException ex) {
          Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(cname);
          engine = (MonitoringEngine) clazz.getDeclaredConstructor().newInstance();
        }
      }
    } catch (Exception ex) {
      Log.error().log(MonitoringManager.class, ex, "unable to load MonitoringEngine '{}'", cname);
    }
    if (engine == null) {
      engine = new NoopMonitoringEngine();
    }

    String quantilesProp = Properties.instance().getString("monitoring.quantiles", "0.5,0.05,0.95,0.01,0.99,0.001");
    List<Quantile> quantileList;
    try {
      String[] quantiles = quantilesProp.split(",");
      if ((quantiles.length % 2) != 0) {
        throw new IllegalArgumentException("invalid quantiles string format, must be "
            + "'quantile1,error1,quantile2,error2,..,quantileN,errorN'");
      }
      quantileList = new ArrayList<>(quantiles.length / 2);
      for (int i = 0; i < quantiles.length; i += 2) {
        quantileList.add(new Quantile(Double.parseDouble(quantiles[i]), Double.parseDouble(quantiles[i + 1])));
      }
    } catch (Exception ex) {
      Log.error().log(MonitoringManager.class, ex, "invalid quantiles string '{}', switch to default value '{}'"
          , quantilesProp, DEF_QUANTILES);
      quantileList = DEF_QUANTILES;
    }

    INSTANCE = new MonitoringManager(engine, quantileList);
    Log.info().log(MonitoringManager.class, "MonitoringManager has been initialized with engine '{}'", engine.getClass().getName());
  }

  public static boolean deregister(Collector collector) {
    return INSTANCE.deregisterImpl(collector);
  }

  public static MonitoringManager instance() {
    return INSTANCE;
  }

  public static char nameSeparator() {
    return INSTANCE.engine.nameSeparator();
  }

  public static <C extends Collector> C register(Supplier<C> supplier) {
    return INSTANCE.registerImpl(supplier);
  }

  public static <C extends Collector> void registerSilent(Supplier<C> supplier) {
    INSTANCE.registerSilentImpl(supplier);
  }

  public static Counter registerCounter(String name, String help, String... tagNames) {
    return (Counter) INSTANCE.register(CollectorType.COUNTER, name, help, tagNames);
  }

  public static Histogram registerHistogram(String name, String help, String... tagNames) {
    return (Histogram) INSTANCE.register(CollectorType.HISTOGRAM, name, help, tagNames);
  }

  public static Timer registerTimer(String name, String help, String... tagNames) {
    return (Timer) INSTANCE.register(CollectorType.TIMER, name, help, tagNames);
  }

  private final MonitoringEngine engine;
  private final List<Quantile> quantiles;

  private MonitoringManager(MonitoringEngine engine, List<Quantile> quantiles) {
    this.engine    = engine;
    this.quantiles = quantiles;
  }

  public List<Quantile> quantiles() {
    return quantiles;
  }

  private boolean deregisterImpl(Collector collector) {
    return engine.deregister(collector);
  }

  private Collector register(CollectorType type, String name, String help, String... tagNames) {
    return type.create(engine,  name, help, tagNames);
  }

  private <C extends Collector> C registerImpl(Supplier<C> supplier) {
    return engine.register(supplier);
  }

  private <C extends Collector> void registerSilentImpl(Supplier<C> supplier) {
    engine.registerSilent(supplier);
  }
}
