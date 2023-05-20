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

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

/**
 * PrintStream implementation of LoggingHandler.
 */
public enum PrintStreamLoggingHandler implements LoggingHandler {
  ERR(System.err),
  OUT(System.out);

  private final PrintStream stream;

  PrintStreamLoggingHandler(PrintStream stream) {
    this.stream = stream;
  }

  @Override public boolean isEnabled(@NotNull Class<?> clazz) {
    return true;
  }

  @Override public void log(@NotNull Class<?> clazz, String message) {
    log(clazz, () -> message, null);
  }

  @Override public void log(@NotNull Class<?> clazz, String message, @Nullable Throwable thrown) {
    log(clazz, () -> message, thrown);
  }

  @Override public void log(@NotNull Class<?> clazz, String message, Object... params) {
    log(clazz, () -> format(message.replaceAll("\\{}", "%s"), params), null);
  }

  @Override public void log(@NotNull Class<?> clazz, Throwable thrown, String message, Object... params) {
    log(clazz, () -> format(message.replaceAll("\\{}", "%s"), params), thrown);
  }

  @Override public void log(@NotNull Class<?> clazz, Supplier<String> message, @Nullable Throwable thrown) {
    synchronized (stream) {
      stream.printf("%s [%s/%s]: %s\n", LocalDateTime.now(), currentThread().getName(), clazz.getSimpleName(), message.get());
      if (thrown != null) {
        thrown.printStackTrace(stream);
      }
    }
  }
}
