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

import org.nightcode.common.base.Objects;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides {@link Monitoring} implementation.
 */
public enum MonitoringProvider implements Monitoring {

  INSTANCE;

  public static Monitoring get() {
    return INSTANCE;
  }

  public static void initialize(MonitoringVisitor monitoringVisitor) {
    Objects.nonNull(monitoringVisitor, "monitoring visitor");
    INSTANCE.setMonitoringVisitor(monitoringVisitor);
  }

  private final ReentrantLock lock = new ReentrantLock();
  private final MonitoringImpl monitoring;

  private MonitoringProvider() {
    monitoring = new MonitoringImpl(NullMonitoringVisitor.getInstance());
  }

  @Override public void visit(String key) {
    monitoring.visit(key);
  }

  @Override public void visit(String key, boolean value) {
    monitoring.visit(key);
  }

  @Override public void visit(String key, double value) {
    monitoring.visit(key);
  }

  @Override public void visit(String key, int value) {
    monitoring.visit(key);
  }

  @Override public void visit(String key, long value) {
    monitoring.visit(key);
  }

  @Override public void visit(String key, String value) {
    monitoring.visit(key);
  }

  @Override public void registerMonitoringComponent(MonitoringComponent monitoringComponent) {
    monitoring.registerMonitoringComponent(monitoringComponent);
  }

  @Override public String serviceName() {
    return monitoring.serviceName();
  }

  @Override public Future<State> start() {
    return monitoring.start();
  }

  @Override public State state() {
    return monitoring.state();
  }

  @Override public Future<State> stop() {
    return monitoring.stop();
  }

  @Override public void unregisterMonitoringComponent(MonitoringComponent monitoringComponent) {
    monitoring.unregisterMonitoringComponent(monitoringComponent);
  }

  private void setMonitoringVisitor(MonitoringVisitor monitoringVisitor) {
    lock.lock();
    try {
      monitoring.setMonitoringVisitor(monitoringVisitor);
    } finally {
      lock.unlock();
    }
  }
}
