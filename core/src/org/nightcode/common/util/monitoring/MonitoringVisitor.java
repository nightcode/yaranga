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

/**
 * Visits each of the events used for retrieving data for Monitoring.
 */
public interface MonitoringVisitor {

  /**
   * Visit a dissociation the value with the specified key.
   *
   * @param key key with which the value is to be dissociated
   */
  void visit(String key);

  /**
   * Visit an association the specified boolean value with the specified key.
   *
   * @param key key with which the value is to be associated
   * @param value value to be associated with key
   */
  void visit(String key, boolean value);

  /**
   * Visit an association the specified double value with the specified key.
   *
   * @param key key with which the value is to be associated
   * @param value value to be associated with key
   */
  void visit(String key, double value);

  /**
   * Visit an association the specified int value with the specified key.
   *
   * @param key key with which the value is to be associated
   * @param value value to be associated with key
   */
  void visit(String key, int value);

  /**
   * Visit an association the specified long value with the specified key.
   *
   * @param key key with which the value is to be associated
   * @param value value to be associated with key
   */
  void visit(String key, long value);

  /**
   * Visit an association the specified string value with the specified key.
   *
   * @param key key with which the value is to be associated
   * @param value value to be associated with key
   */
  void visit(String key, String value);
}
