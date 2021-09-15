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

import org.nightcode.common.annotations.Beta;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.nightcode.common.util.props.PropertiesStorage.Type;

/**
 * Main properties class.
 */
@Beta
public final class Properties {

  private static final Properties INSTANCE = new Properties();

  public static Properties instance() {
    return INSTANCE;
  }

  private volatile PropertiesStorage storage = PropertiesEmptyStorage.instance();

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ConcurrentMap<String, Property> properties = new ConcurrentHashMap<>();

  private Properties() {
    // do nothing
  }

  public boolean getBooleanValue(String key) {
    Property property = properties.computeIfAbsent(key, k -> readProperty(k, Type.BOOLEAN, PropertiesStorage.THROW));
    if (!property.hasBooleanValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type boolean");
    }
    return property.getBooleanValue();
  }

  public boolean getBooleanValue(String key, boolean def) {
    Property property = properties.computeIfAbsent(key, k -> {
      Property p = readProperty(k, Type.BOOLEAN, PropertiesStorage.NULL_POLICY);
      if (p == null) {
        p = Property.createBoolean(def);
      }
      return p;
    });
    if (!property.hasBooleanValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type boolean");
    }
    return property.getBooleanValue();
  }

  public byte getByteValue(String key) {
    Property property = properties.computeIfAbsent(key, k -> readProperty(k, Type.BYTE, PropertiesStorage.THROW));
    if (!property.hasByteValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type byte");
    }
    return property.getByteValue();
  }

  public byte getByteValue(String key, byte def) {
    Property property = properties.computeIfAbsent(key, k -> {
      Property p = readProperty(k, Type.BYTE, PropertiesStorage.NULL_POLICY);
      if (p == null) {
        p = Property.createByte(def);
      }
      return p;
    });
    if (!property.hasByteValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type byte");
    }
    return property.getByteValue();
  }

  public <T> Collection<T> getCollectionValue(String key, Class<T> clazz) {
    Property property = properties.computeIfAbsent(key, k -> readProperty(k, Type.COLLECTION, PropertiesStorage.THROW));
    if (!property.hasCollectionValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type Collection");
    }
    return (Collection<T>) property.getCollectionValue();
  }

  public <T> Collection<T> getCollectionValue(String key, Class<T> clazz, Collection<T> def) {
    Property property = properties.computeIfAbsent(key, k -> {
      Property p = readProperty(k, Type.COLLECTION, PropertiesStorage.NULL_POLICY);
      if (p == null) {
        p = Property.createCollection(def);
      }
      return p;
    });
    if (!property.hasCollectionValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type Collection");
    }
    return (Collection<T>) property.getCollectionValue();
  }

  public int getIntValue(String key) {
    Property property = properties.computeIfAbsent(key, k -> readProperty(k, Type.INT, PropertiesStorage.THROW));
    if (!property.hasIntValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type int");
    }
    return property.getIntValue();
  }

  public int getIntValue(String key, int def) {
    Property property = properties.computeIfAbsent(key, k -> {
      Property p = readProperty(k, Type.INT, PropertiesStorage.NULL_POLICY);
      if (p == null) {
        p = Property.createInt(def);
      }
      return p;
    });
    if (!property.hasIntValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type int");
    }
    return property.getIntValue();
  }

  public long getLongValue(String key) {
    Property property = properties.computeIfAbsent(key, k -> readProperty(k, Type.LONG, PropertiesStorage.THROW));
    if (!property.hasLongValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type long");
    }
    return property.getLongValue();
  }

  public long getLongValue(String key, long def) {
    Property property = properties.computeIfAbsent(key, k -> {
      Property p = readProperty(k, Type.LONG, PropertiesStorage.NULL_POLICY);
      if (p == null) {
        p = Property.createLong(def);
      }
      return p;
    });
    if (!property.hasLongValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type long");
    }
    return property.getLongValue();
  }

  public String getStringValue(String key) {
    Property property = properties.computeIfAbsent(key, k -> readProperty(k, Type.STRING, PropertiesStorage.THROW));
    if (!property.hasStringValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type String");
    }
    return property.getStringValue();
  }

  public String getStringValue(String key, String def) {
    Property property = properties.computeIfAbsent(key, k -> {
      Property p = readProperty(k, Type.STRING, PropertiesStorage.NULL_POLICY);
      if (p == null) {
        p = Property.createString(def);
      }
      return p;
    });
    if (!property.hasStringValue()) {
      throw new IllegalStateException("unable to get <" + key + "> of type String");
    }
    return property.getStringValue();
  }

  public void setPropertiesStorage(PropertiesStorage storage) {
    lock.writeLock().lock();
    try {
      this.storage = storage;
      properties.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }

  private Property readProperty(String key, Type type, PropertiesStorage.NotFoundPolicy notFoundPolicy) {
    lock.readLock().lock();
    try {
      PropertiesStorage st = storage;
      return st.readProperty(key, type, notFoundPolicy);
    } catch (PropertyException ex) {
      throw new IllegalStateException(ex);
    } finally {
      lock.readLock().unlock();
    }
  }
}
