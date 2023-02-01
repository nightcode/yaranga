package org.nightcode.common.base;

import org.nightcode.common.annotations.Beta;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

/**
 * todo.
 */
@Beta
public final class Jvm {

  private static final long SECOND = TimeUnit.SECONDS.toMillis(1);
  private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
  private static final long HOUR   = TimeUnit.HOURS.toMillis(1);
  private static final long DAY    = TimeUnit.DAYS.toMillis(1);

  private final static RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();
  private final static ThreadMXBean  THREAD_MX_BEAN  = ManagementFactory.getThreadMXBean();

  private static final int PID = getPid();

  private static final String BOOT_CLASSPATH = RUNTIME_MX_BEAN.getBootClassPath();
  private static final String CLASSPATH      = RUNTIME_MX_BEAN.getClassPath();
  private static final String LIBRARY_PATH   = RUNTIME_MX_BEAN.getLibraryPath();
  private static final String VM_ARGUMENTS   = getVmArguments();

  public static String bootClassPath() {
    return BOOT_CLASSPATH;
  }

  public static String classPath() {
    return CLASSPATH;
  }

  public static String libraryPath() {
    return LIBRARY_PATH;
  }

  public static int pid() {
    return PID;
  }

  public static String uptime() {
    long uptime = RUNTIME_MX_BEAN.getUptime();
    String formatted;
    if (uptime < MINUTE) {
      formatted = uptime / SECOND + " s";
    } else {
      long days = uptime / DAY;
      long remainingHours = uptime % DAY;
      long hours = remainingHours / HOUR;
      long remainingMinutes = remainingHours % HOUR;
      long minutes = remainingMinutes / MINUTE;
      if (uptime >= DAY) {
        formatted = days + " d, " + hours + " h, " + minutes + " m";
      } else if (uptime >= HOUR) {
        formatted = hours + " h, " + minutes + " m";
      } else {
        formatted = minutes + " m";
      }
    }
    return formatted;
  }

  public static long uptimeMs() {
    return RUNTIME_MX_BEAN.getUptime();
  }

  public static String vmArguments() {
    return VM_ARGUMENTS;
  }

  private static int getPid() {
    String pid = null;

    File procSelf = new File("/proc/self");
    if (procSelf.exists()) {
      try {
        pid = procSelf.getCanonicalFile().getName();
      } catch (IOException ignore) {
        // do nothing
      }
    }

    if (pid == null) {
      pid = RUNTIME_MX_BEAN.getName().split("@")[0];
    }

    if (pid != null) {
      try {
        return Integer.parseInt(pid);
      } catch (NumberFormatException ignore) {
        // do nothing
      }
    }

    int p = (int) (System.currentTimeMillis() / 1000);
    System.err.printf("unable to get real PID, used %s instead%n", p);
    return p;
  }

  private static String getVmArguments() {
    return String.join(" ", RUNTIME_MX_BEAN.getInputArguments());
  }

  private Jvm() {
    // do nothing
  }
}
