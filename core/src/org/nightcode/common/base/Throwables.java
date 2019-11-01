/*
 * Copyright (C) 2008 The NightCode Open Source Project
 *
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

package org.nightcode.common.base;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Throwables helper class.
 */
public final class Throwables {

  private Throwables() {
    // do nothing
  }

  public static Throwable getRootCause(Throwable throwable) {
    Throwable predecessor = throwable;
    Throwable cause;
    boolean movePredecessor = false;
    while ((cause = throwable.getCause()) != null) {
      if (cause == predecessor) {
        throw new IllegalStateException("loop in casual chain", cause);
      }
      if (movePredecessor) {
        predecessor = predecessor.getCause();
      }
      movePredecessor = !movePredecessor;
      throwable = cause;
    }
    return throwable;
  }

  public static String getStackTrace(Throwable throwable) {
    StringWriter out = new StringWriter();
    throwable.printStackTrace(new PrintWriter(out));
    return out.toString();
  }

  public static RuntimeException propagate(Throwable throwable) {
    java.util.Objects.requireNonNull(throwable);
    if (Error.class.isInstance(throwable)) {
      throw Error.class.cast(throwable);
    } else if (RuntimeException.class.isInstance(throwable)) {
      throw RuntimeException.class.cast(throwable);
    }
    throw new RuntimeException(throwable);
  }
}
