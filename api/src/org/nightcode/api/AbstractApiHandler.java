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

package org.nightcode.api;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract API handler.
 */
public abstract class AbstractApiHandler implements ApiHandler {

  private static final class ObjectMap<E> {
    private static class Core {
      final int shift;
      final int length;
      final String[] keys;
      final Object[] values;

      Core(int shift) {
        this.shift = shift;
        length = 1 << (32 - shift);
        keys = new String[length];
        values = new Object[length];
      }
    }

    private static final int MAGIC = 0xB46394CD;
    private static final int MAX_SHIFT = 29;
    private static final int THRESHOLD = (int) (1L << 31);

    private volatile Core core = new Core(MAX_SHIFT);
    private volatile int size;

    E get(@NotNull String key) {
      Core core = this.core;
      int i = (key.hashCode() * MAGIC) >>> core.shift;
      String k;
      while (!key.equals(k = core.keys[i])) {
        if (k == null) {
          return null;
        }
        if (i == 0) {
          i = core.length;
        }
        i--;
      }
      // noinspection unchecked
      return (E) core.values[i];
    }

    // need external synchronization
    public void put(@NotNull String key, E value) {
      if (putInternal(this.core, key, value)) {
        if (++size > (THRESHOLD >>> this.core.shift)) {
          rehash();
        }
      }
    }

    private boolean putInternal(Core core, String key, Object value) {
      int i = (key.hashCode() * MAGIC) >>> core.shift;
      String k;
      while (!key.equals(k = core.keys[i])) {
        if (k == null) {
          core.keys[i] = key;
          core.values[i] = value;
          return true;
        }
        if (i == 0) {
          i = core.length;
        }
        i--;
      }
      core.values[i] = value;
      return false;
    }

    private void rehash() {
      Core oldCore = core;
      Core newCore = new Core(oldCore.shift - 1);
      for (int i = 0; i < oldCore.length; i++) {
        if (oldCore.keys[i] != null) {
          putInternal(newCore, oldCore.keys[i], oldCore.values[i]);
        }
      }
      core = newCore;
    }
  }

  private final ObjectMap<Class<? extends Message>> classes = new ObjectMap<>();
  private final ObjectMap<MethodHandler<? extends Message, ? extends Message>> handlers = new ObjectMap<>();

  @Override public Class<? extends Message> findClass(String typeUrl) {
    return classes.get(typeUrl);
  }

  @Override public <Q extends Message, R extends Message> MethodHandler<Q, R> handlerFor(String typeUrl) {
    // noinspection unchecked
    return (MethodHandler<Q, R>) handlers.get(typeUrl);
  }

  protected <Q extends Message, R extends Message> void registerMethod(Q message, MethodHandler<Q, R> methodHandler) {
    String typeUrl = Any.pack(message, name()).getTypeUrl();
    synchronized (classes) {
      classes.put(typeUrl, message.getClass());
    }
    synchronized (handlers) {
      handlers.put(typeUrl, methodHandler);
    }
  }
}
