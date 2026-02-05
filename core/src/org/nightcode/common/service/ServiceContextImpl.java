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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.nightcode.common.lang.Event;
import org.nightcode.common.logging.Log;

/**
 * ServiceContext implementation.
 */
public class ServiceContextImpl implements ServiceContext, Service.StateListener {

  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  private final Map<Class<?>, Object>         singletons = new HashMap<>();
  private final CopyOnWriteArrayList<Service> services   = new CopyOnWriteArrayList<>();

  @Override public void close() {
    if (!shutdown.compareAndSet(false, true)) {
      return;
    }

    Map<Service, Future<Service>> futures = new HashMap<>();
    // noinspection unchecked
    List<Service> list = (List<Service>) services.clone();
    for (final Service service : list) {
      futures.put(service, service.stopAsync());
    }
    futures.forEach((k, v) -> {
      try {
        v.get(60, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        Log.warn().log(ServiceContextImpl.class, ex, "can not stop service {}, operation has been interrupted", k);
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        Log.warn().log(ServiceContextImpl.class, ex, "can not stop service {}", k);
      }
    });
    services.removeAll(list);
  }

  @Override public void onEvent(Event<Service, Service.State> event) {
    if (Service.State.RUNNING.equals(event.type())) {
      services.add(event.subject());
    } else if (Service.State.TERMINATED.equals(event.type()) || Service.State.FAILED.equals(event.type())) {
      services.remove(event.subject());
    }
  }

  @Override public synchronized <T> T registerService(Class<? super T> clazz, Supplier<T> serviceSupplier) {
    return computeIfAbsent(clazz, cl -> {
      T service = serviceSupplier.get();
      if (service instanceof Service s) {
        s.addEventListener(this);
        if (s.isRunning()) {
          services.add(s);
        }
      }
      return service;
    });
  }

  private <T> T computeIfAbsent(Class<? super T> clazz, Function<? super Class<? super T>, ? extends T> mappingFunction) {
    T service;
    // noinspection unchecked
    if ((service = (T) singletons.get(clazz)) == null) {
      T newService;
      if ((newService = mappingFunction.apply(clazz)) != null) {
        singletons.put(clazz, newService);
        return newService;
      }
    }
    return service;
  }
}
