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

package org.nightcode.common.util.props;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Composite implementation of PropertiesStorage.
 */
public class CompositePropertiesStorage implements PropertiesStorage {

  private List<PropertiesStorage> storages;

  public CompositePropertiesStorage(PropertiesStorage... storages) {
    this.storages = Collections.unmodifiableList(Arrays.asList(storages));
  }

  @Override public boolean readBoolean(String key) throws PropertyException {
    for (PropertiesStorage storage : storages) {
      try {
        return storage.readBoolean(key);
      } catch (PropertyException ex) {
        // ignore this exception and try next storage
      }
    }
    throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);
  }

  @Override public byte readByte(String key) throws PropertyException {
    for (PropertiesStorage storage : storages) {
      try {
        return storage.readByte(key);
      } catch (PropertyException ex) {
        // ignore this exception and try next storage
      }
    }
    throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);  }

  @Override public int readInt(String key) throws PropertyException {
    for (PropertiesStorage storage : storages) {
      try {
        return storage.readInt(key);
      } catch (PropertyException ex) {
        // ignore this exception and try next storage
      }
    }
    throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);  }

  @Override public long readLong(String key) throws PropertyException {
    for (PropertiesStorage storage : storages) {
      try {
        return storage.readLong(key);
      } catch (PropertyException ex) {
        // ignore this exception and try next storage
      }
    }
    throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);  }

  @Override public String readString(String key) throws PropertyException {
    for (PropertiesStorage storage : storages) {
      try {
        return storage.readString(key);
      } catch (PropertyException ex) {
        // ignore this exception and try next storage
      }
    }
    throw new PropertyException(PropertyException.ErrorCode.PROPERTY_NOT_FOUND);  }
}
