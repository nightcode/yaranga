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

package org.nightcode.common.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.nightcode.common.lang.ThrowingRunnable;
import org.nightcode.common.logging.Log;

/**
 * Executor utils.
 */
public enum ExecutorUtils {
  ;

  private static final class NamedThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final String      name;
    private final String      threadPrefix;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private NamedThreadFactory(String prefix) {
      name         = prefix;
      threadPrefix = prefix + "-thread-";
      group        = Thread.currentThread().getThreadGroup();
    }

    @Override public Thread newThread(@NotNull Runnable r) {
      Thread thread = new Thread(group, r, threadPrefix + threadNumber.getAndIncrement(), 0);
      if (thread.isDaemon()) {
        thread.setDaemon(false);
      }
      if (thread.getPriority() != Thread.NORM_PRIORITY) {
        thread.setPriority(Thread.NORM_PRIORITY);
      }
      return thread;
    }

    @Override public String toString() {
      return name;
    }
  }

  private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

  private static volatile Function<ThreadPoolExecutor, ThreadPoolExecutor> executorInterceptor = t -> t;
  private static volatile Consumer<ExecutorService>                        cleaner             = t -> { };

  static void initialize(Function<ThreadPoolExecutor, ThreadPoolExecutor> executorInterceptor, Consumer<ExecutorService> cleaner) {
    if (INITIALIZED.compareAndSet(false, true)) {
      ExecutorUtils.executorInterceptor = executorInterceptor;
      ExecutorUtils.cleaner             = cleaner;
    } else {
      throw new IllegalArgumentException("already initialized");
    }
  }

  private static <T extends ThreadPoolExecutor> T intercept(T src) {
    // noinspection unchecked
    return (T) executorInterceptor.apply(src);
  }

  public static ThreadFactory namedThreadFactory(String prefix) {
    return new NamedThreadFactory(prefix);
  }

  public static ExecutorService fixedThreadPool(String executorName, int nThreads) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static ExecutorService fixedThreadPool(String executorName, int nThreads, RejectedExecutionHandler rejectedExecutionHandler) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()
        , threadFactory, rejectedExecutionHandler) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static ExecutorService fixedThreadPool(String executorName, int nThreads, int queueSize,
                                                RejectedExecutionHandler rejectedExecutionHandler) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS
        , new LinkedBlockingQueue<>(queueSize), threadFactory
        , rejectedExecutionHandler) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static ExecutorService singleThreadExecutor(String executorName) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static ExecutorService singleThreadExecutor(String executorName, RejectedExecutionHandler rejectedExecutionHandler) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS
        , new LinkedBlockingQueue<>(), threadFactory, rejectedExecutionHandler) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static ScheduledExecutorService scheduledExecutorService(String executorName, int nThreads) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ScheduledThreadPoolExecutor(nThreads, threadFactory) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static ScheduledExecutorService scheduledExecutorService(String executorName, int nThreads,
                                                                  RejectedExecutionHandler rejectedExecutionHandler) {
    ThreadFactory threadFactory = ExecutorUtils.namedThreadFactory(executorName + "-executor");
    return intercept(new ScheduledThreadPoolExecutor(nThreads, threadFactory, rejectedExecutionHandler) {
      @Override public String toString() {
        return executorName + "-executor:" + super.toString();
      }
    });
  }

  public static boolean shutdown(ExecutorService executor) {
    return shutdownGracefully(executor, 60, TimeUnit.SECONDS);
  }

  public static boolean shutdownGracefully(ExecutorService executor, long duration, TimeUnit unit) {
    try {
      executor.shutdown();
      long    timeoutNanos  = TimeUnit.SECONDS.toNanos(10);
      long    durationNanos = unit.toNanos(duration);
      long    iterations    = durationNanos / timeoutNanos;
      boolean terminated    = false;
      for (int i = 0; i < iterations && !terminated; i++) {
        terminated = executor.awaitTermination(timeoutNanos, TimeUnit.NANOSECONDS);
        if (!terminated) {
          Log.info().log(ExecutorUtils.class, "{} the timeout elapsed before termination, iteration {} of {}", executor, i + 1, iterations);
        }
      }
      if (!executor.isTerminated()) {
        List<Runnable> neverCommencedExecution = executor.shutdownNow();
        for (Runnable r : neverCommencedExecution) {
          Log.warn().log(ExecutorUtils.class, "{}: shutdown now {}", executor, r);
        }
      }
    } catch (InterruptedException ex) {
      List<Runnable> neverCommencedExecution = executor.shutdownNow();
      for (Runnable r : neverCommencedExecution) {
        Log.warn().log(ExecutorUtils.class, "{}: shutdown now {}", executor, r);
      }
      Thread.currentThread().interrupt();
    } finally {
      cleaner.accept(executor);
    }
    Log.info().log(ExecutorUtils.class, "{}: terminated", executor);
    return executor.isTerminated();
  }

  public static void sleepUninterruptibly(long delay, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(delay);
      long end            = Clock.sys().nanoTime() + remainingNanos;
      while (true) {
        try {
          TimeUnit.NANOSECONDS.sleep(remainingNanos);
          return;
        } catch (InterruptedException ignore) {
          interrupted    = true;
          remainingNanos = end - Clock.sys().nanoTime();
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static <E extends Exception> void safelyExecute(ThrowingRunnable<E> r) {
    safelyExecute(r, "exception occurred during execution");
  }

  public static <E extends Exception> void safelyExecute(ThrowingRunnable<E> r, String message) {
    try {
      r.run();
    } catch (Exception e) {
      Log.warn().log(ExecutorUtils.class, message, e);
    }
  }
}
