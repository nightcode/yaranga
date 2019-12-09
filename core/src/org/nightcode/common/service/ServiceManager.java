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

import org.nightcode.common.util.logging.LogManager;
import org.nightcode.common.util.logging.Logger;

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

  private static final Logger LOGGER = LogManager.getLogger(ServiceManager.class);

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
    LOGGER.config("[ServiceManager]: shutdown hook for service <%s> has been added", service.serviceName());
  }

  public void removeShutdownHook(Service service) {
    Objects.requireNonNull(service, "service");
    Service serv = services.remove(service.serviceName());
    if (serv == null) {
      LOGGER.info("[ServiceManager]: service <%s> has never been added", service.serviceName());
    } else {
      LOGGER.config("[ServiceManager]: shutdown hook for service <%s> has been removed", service.serviceName());
    }
  }

  public void shutdownAll() {
    LOGGER.info("[ServiceManager]: external termination in progress..");
    Map<String, Future<Service.State>> futures = new HashMap<>();
    for (final Service service : services.values()) {
        futures.put(service.serviceName(), service.stop());
    }
    futures.forEach((key, value) -> {
      try {
        value.get();
      } catch (Exception ex) {
        LOGGER.warn(ex, "[ServiceManager]: cannot stop service <%s>", key);
      }

    });
    services.clear();
  }

  public void shutdownAll(long timeout, TimeUnit unit) {
    LOGGER.info("[ServiceManager]: external termination in progress..");
    Map<String, Future<Service.State>> futures = new HashMap<>();
    for (final Service service : services.values()) {
        futures.put(service.serviceName(), service.stop());
    }
    futures.forEach((key, value) -> {
      try {
        value.get(timeout, unit);
      } catch (Exception ex) {
        LOGGER.warn(ex, "[ServiceManager]: cannot stop service <%s>", key);
      }
    });
    services.clear();
  }
}
