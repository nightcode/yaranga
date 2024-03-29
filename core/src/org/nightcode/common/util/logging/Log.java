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

package org.nightcode.common.util.logging;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Log helper class.
 */
public enum Log {
  ;

  private static final AtomicReference<LoggingHandler> TRACE = new AtomicReference<>(PrintStreamLoggingHandler.OUT);
  private static final AtomicReference<LoggingHandler> DEBUG = new AtomicReference<>(PrintStreamLoggingHandler.OUT);
  private static final AtomicReference<LoggingHandler> INFO  = new AtomicReference<>(PrintStreamLoggingHandler.OUT);
  private static final AtomicReference<LoggingHandler> WARN  = new AtomicReference<>(PrintStreamLoggingHandler.ERR);
  private static final AtomicReference<LoggingHandler> ERROR = new AtomicReference<>(PrintStreamLoggingHandler.ERR);
  private static final AtomicReference<LoggingHandler> FATAL = new AtomicReference<>(PrintStreamLoggingHandler.ERR);

  public static LoggingHandler debug() {
    return DEBUG.get();
  }

  public static LoggingHandler error() {
    return ERROR.get();
  }

  public static LoggingHandler fatal() {
    return FATAL.get();
  }

  public static LoggingHandler info() {
    return INFO.get();
  }

  public static LoggingHandler trace() {
    return TRACE.get();
  }

  public static LoggingHandler warn() {
    return WARN.get();
  }

  public static void setLoggingHandler(LoggingHandler trace,
                                       LoggingHandler debug,
                                       LoggingHandler info,
                                       LoggingHandler warn,
                                       LoggingHandler error,
                                       LoggingHandler fatal) {
    TRACE.set(trace);
    DEBUG.set(debug);
    INFO.set(info);
    WARN.set(warn);
    ERROR.set(error);
    FATAL.set(fatal);
  }
}
