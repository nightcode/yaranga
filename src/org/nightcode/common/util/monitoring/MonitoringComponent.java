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

package org.nightcode.common.util.monitoring;

import java.io.IOException;

/**
 * A {@link MonitoringComponent} provides events for monitoring.
 */
public interface MonitoringComponent {

  /**
   * Retrieves data from monitoring component.
   *
   * @param visitor monitoring visitor
   * @throws IOException if data cannot be retrieved
   */
  void retrieveData(MonitoringVisitor visitor) throws IOException;
}
