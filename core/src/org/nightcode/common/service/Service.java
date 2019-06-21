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

import java.util.concurrent.Future;

/**
 * An object that provides methods that can produce a Future for tracking
 * progress of {@link #start starting } or {@link #stop stopping}.
 */
public interface Service {

  /**
   * Public states of a service.
   */
  enum State {
    NEW,
    RUNNING,
    TERMINATED
  }

  /**
   * Returns the service name.
   *
   * @return the service name
   */
  String serviceName();

  /**
   * Starts the service.
   * @return a Future representing the result of service's startup.
   */
  Future<State> start();

  /**
   * Stops the service.
   *
   * @return a Future representing the result of service's shutdown.
   */
  Future<State> stop();
}
