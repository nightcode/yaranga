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

package org.nightcode.common.util.monitoring;

import org.nightcode.common.service.AbstractThreadService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides default implementation of {@link Monitoring} interface.
 */
@Singleton
public final class MonitoringImpl extends AbstractThreadService implements Monitoring {

  static final String DEF_RETRIEVE_TIMEOUT = "5000";

  private final List<MonitoringComponent> monitoringComponents = new CopyOnWriteArrayList<>();

  private volatile MonitoringVisitor monitoringVisitor;
  private final long retrieveTimeout;

  @Inject
  MonitoringImpl(MonitoringVisitor monitoringVisitor) {
    super("SystemMonitoring");
    this.monitoringVisitor = monitoringVisitor;
    retrieveTimeout = Long.parseLong(System.getProperty("configuration.monitoring.RetrieveTimeoutMs"
        , DEF_RETRIEVE_TIMEOUT));
  }

  @Override public void registerMonitoringComponent(MonitoringComponent monitoringComponent) {
    Objects.requireNonNull(monitoringComponent, "monitoring component");
    monitoringComponents.add(monitoringComponent);
  }

  @Override public void visit(String key) {
    monitoringVisitor.visit(key);
  }

  @Override public void visit(String key, boolean value) {
    monitoringVisitor.visit(key, value);
  }

  @Override public void visit(String key, double value) {
    monitoringVisitor.visit(key, value);
  }

  @Override public void visit(String key, int value) {
    monitoringVisitor.visit(key, value);
  }

  @Override public void visit(String key, long value) {
    monitoringVisitor.visit(key, value);
  }

  @Override public void visit(String key, String value) {
    monitoringVisitor.visit(key, value);
  }

  @Override public void unregisterMonitoringComponent(MonitoringComponent monitoringComponent) {
    monitoringComponents.remove(monitoringComponent);
  }

  @Override protected void service() throws Exception {
    for (MonitoringComponent monitoringComponent : monitoringComponents) {
      try {
        monitoringComponent.retrieveData(monitoringVisitor);
      } catch (IOException ex) {
        logger.warn(ex, "cannot retrieve data from component <%s>", monitoringComponent);
      }
    }
    Thread.sleep(retrieveTimeout);
  }

  void setMonitoringVisitor(MonitoringVisitor monitoringVisitor) {
    this.monitoringVisitor = monitoringVisitor;
  }
}
