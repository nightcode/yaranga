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

import java.util.concurrent.CompletableFuture;

import org.nightcode.common.lang.Event;
import org.nightcode.common.lang.EventListener;

/**
 * An object that provides methods that can produce a Future for tracking
 * progress of {@link #startAsync starting } or {@link #stopAsync stopping}.
 */
public interface Service {

  /**
   * Public states of a service.
   */
  enum State {
    NEW,
    STARTING,
    RUNNING,
    STOPPING,
    TERMINATED,
    FAILED;
  }

  interface StateListener extends EventListener<Service, State> {
    void onEvent(Event<Service, State> event);
  }

  /**
   * Registers the state listener.
   *
   * @param listener the state listener
   */
  void addEventListener(StateListener listener);

  /**
   * Returns reason of the service failure.
   *
   * @return reason of the service failure
   */
  Throwable failureCause();

  /**
   * Returns true if the service is running, else returns false.
   *
   * @return true if the service is running
   */
  boolean isRunning();

  /**
   * Deregisters the state listener.
   *
   * @param listener the state listener
   */
  void removeEventListener(StateListener listener);

  /**
   * Returns service name.
   *
   * @return service name
   */
  String serviceName();

  /**
   * Starts the service.
   *
   * @return a Future representing the result of service's startup.
   */
  CompletableFuture<Service> startAsync();

  /**
   * Returns the service state.
   *
   * @return the service state
   */
  State state();

  /**
   * Stops the service.
   *
   * @return a Future representing the result of service's shutdown.
   */
  CompletableFuture<Service> stopAsync();
}
