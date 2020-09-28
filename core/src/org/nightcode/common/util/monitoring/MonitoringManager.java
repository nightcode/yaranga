package org.nightcode.common.util.monitoring;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.util.logging.LogManager;
import org.nightcode.common.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Base class for monitoring.
 */
@Beta
public final class MonitoringManager {

  private static final MonitoringManager INSTANCE;

  private static final Logger LOGGER = LogManager.getLogger(MonitoringManager.class);

  static {
    MonitoringProvider provider = null;
    String cname = null;
    try {
      cname = System.getProperty("yaranga.monitoring.provider");
      if (cname != null) {
        try {
          Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(cname);
          provider = (MonitoringProvider) clazz.newInstance();
        } catch (ClassNotFoundException ex) {
          Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(cname);
          provider = (MonitoringProvider) clazz.newInstance();
        }
      }
    } catch (Exception ex) {
      LOGGER.error(ex, "unable to load MonitoringProvider '%s'", cname);
    }

    if (provider == null) {
      provider = new NullMonitoringProvider();
    }

    String prefix = System.getProperty("yaranga.monitoring.prefix");
    INSTANCE = new MonitoringManager(prefix, provider);
    LOGGER.config("MonitoringManager has been initialized with provider '%s'", provider.name());
  }

  public static String prefix() {
    return INSTANCE.prefix;
  }

  public static Counter registerCounter(String name) {
    return INSTANCE.provider.registerCounter(name(name));
  }

  public static <T> void registerGauge(String name, Gauge<T> gauge) {
    INSTANCE.provider.registerGauge(name(name), gauge);
  }

  public static Histogram registerHistogram(String name) {
    return INSTANCE.provider.registerHistogram(name(name));
  }

  public static Timer registerTimer(String name) {
    return INSTANCE.provider.registerTimer(name(name));
  }

  public static void deregister(String name) {
    INSTANCE.provider.deregister(name(name));
  }

  public static MonitoringManager instance() {
    return INSTANCE;
  }

  private static String name(String name) {
    return INSTANCE.prefix + "." + name;
  }

  private final String prefix;
  private final MonitoringProvider provider;

  private MonitoringManager(@Nullable String prefix, MonitoringProvider provider) {
    this.provider = provider;
    if (prefix == null) {
      prefix = "";
    }
    this.prefix = prefix;
  }

  public MonitoringProvider provider() {
    return provider;
  }
}
