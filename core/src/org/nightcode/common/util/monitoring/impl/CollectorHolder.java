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
import org.nightcode.common.util.monitoring.Collector;

import java.util.Collection;

/**
 * A tag interface for collector holder classes.
 */
@Beta
interface CollectorHolder extends Collector {

  void addChild(Collector child, String... tagValues);

  Collection<Collector> children();

  Collector getChild(String... tagValues);

  Collector target();

  CollectorType type();
}
