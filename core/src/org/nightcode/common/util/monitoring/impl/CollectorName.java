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

package org.nightcode.common.util.monitoring.impl;

import org.nightcode.common.annotations.Beta;

import java.util.Arrays;
import java.util.Objects;

/**
 * Helper class to create name of a collector.
 */
@Beta
public final class CollectorName {

  private static final String SUBSYSTEM;
  private static final char SEPARATOR = MonitoringManager.nameSeparator();

  private static final String[] EMPTY_ARRAY = new String[] {};

  static {
    String subsystem = MonitoringManager.subsystem();
    SUBSYSTEM = (subsystem.isEmpty()) ? "" : subsystem + SEPARATOR;
  }

  public static CollectorName build(String name) {
    return new CollectorName(name);
  }

  public static CollectorName build(String name, String... tagNames) {
    return new CollectorName(name, tagNames);
  }

  public static CollectorName of(CollectorName name, String... tagValues) {
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append(name.name);
    for (int i = 0; i < name.tagNames().length; i++) {
      nameBuilder.append(SEPARATOR).append(name.tagNames()[i]).append(SEPARATOR).append(tagValues[i]);
    }
    return new CollectorName(nameBuilder.toString());
  }

  public static String name(String name, String... parts) {
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append(name);
    for (String part : parts) {
      nameBuilder.append(SEPARATOR).append(part);
    }
    return nameBuilder.toString();
  }

  private final String name;
  private final String fullName;
  private final String[] tagNames;

  private CollectorName(String name) {
    this(name, EMPTY_ARRAY);
  }

  private CollectorName(String name, String... tagNames) {
    this.name = name;
    this.fullName = SUBSYSTEM + name;
    this.tagNames = tagNames;
  }

  public String fullName() {
    return fullName;
  }

  public String name() {
    return name;
  }

  public String[] tagNames() {
    return tagNames;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CollectorName other = (CollectorName) obj;
    return Objects.equals(fullName, other.fullName);
  }

  @Override public int hashCode() {
    return Objects.hash(fullName);
  }

  @Override public String toString() {
    return "CollectorName{name='" + name
        + "', fullName='" + fullName
        + "', tagNames=" + Arrays.toString(tagNames)
        + '}';
  }
}
