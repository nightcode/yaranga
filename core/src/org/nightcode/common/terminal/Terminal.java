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

package org.nightcode.common.terminal;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base terminal interface.
 */
public interface Terminal {

  AtomicReference<Terminal> INSTANCE = new AtomicReference<>();

  static void clear() {
    INSTANCE.set(null);
  }

  static Terminal get() {
    return INSTANCE.get();
  }

  static void set(Terminal terminal) {
    INSTANCE.set(terminal);
  }

  void printf(String format, Object... args);

  String readLine(String format, Object... args);
}
