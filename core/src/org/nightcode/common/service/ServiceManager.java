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

import org.nightcode.common.util.logging.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

/**
 * Service manager.
 */
@Singleton
public final class ServiceManager {

  private static final ServiceManager INSTANCE = new ServiceManager();

  public static ServiceManager instance() {
    return INSTANCE;
  }

  private final ConcurrentMap<String, Service> services = new ConcurrentHashMap<>();

  // NOTE: package private because of DI frameworks
  ServiceManager() {
    // do nothing
  }

  public void addShutdownHook(Service service) {
    Objects.requireNonNull(service, "service");
    Service serv = services.putIfAbsent(service.serviceName(), service);
    if (serv != null) {
      throw new IllegalStateException("service <" + service.serviceName() + "> has already been added");
    }
    Log.info().log(getClass(), "[ServiceManager]: shutdown hook for service <{}> has been added", service.serviceName());
  }

  public void removeShutdownHook(Service service) {
    Objects.requireNonNull(service, "service");
    Service serv = services.remove(service.serviceName());
    if (serv == null) {
      Log.info().log(getClass(), "[ServiceManager]: service <{}> has never been added", service.serviceName());
    } else {
      Log.info().log(getClass(), "[ServiceManager]: shutdown hook for service <{}> has been removed", service.serviceName());
    }
  }

  public void shutdownAll() {
    Log.info().log(getClass(), "[ServiceManager]: external termination in progress..");
    Map<String, Future<Service.State>> futures = new HashMap<>();
    for (final Service service : services.values()) {
        futures.put(service.serviceName(), service.stop());
    }
    futures.forEach((key, value) -> {
      try {
        value.get();
      } catch (Exception ex) {
        Log.warn().log(getClass(), ex, "[ServiceManager]: cannot stop service <{}>", key);
      }

    });
    services.clear();
  }

  public void shutdownAll(long timeout, TimeUnit unit) {
    Log.info().log(getClass(), "[ServiceManager]: external termination in progress..");
    Map<String, Future<Service.State>> futures = new HashMap<>();
    for (final Service service : services.values()) {
        futures.put(service.serviceName(), service.stop());
    }
    futures.forEach((key, value) -> {
      try {
        value.get(timeout, unit);
      } catch (Exception ex) {
        Log.warn().log(getClass(), ex, "[ServiceManager]: cannot stop service <{}>", key);
      }
    });
    services.clear();
  }
}
