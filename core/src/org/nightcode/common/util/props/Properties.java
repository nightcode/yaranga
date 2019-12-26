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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main properties class.
 */
public final class Properties {

  private enum Type {
    BOOLEAN,
    BYTE,
    INT,
    LONG,
    STRING
  }

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
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.BOOLEAN);
      if (property == null) {
        throw new IllegalStateException("can't get <" + key + "> of type boolean");
      }
    }
    if (!property.hasBooleanValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type boolean");
    }
    return property.getBooleanValue();
  }

  public boolean getBooleanValue(String key, boolean def) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.BOOLEAN);
      if (property == null) {
        property = Property.createBoolean(def);
        properties.put(key, property);
      }
    }
    if (!property.hasBooleanValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type boolean");
    }
    return property.getBooleanValue();
  }

  public byte getByteValue(String key) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.BYTE);
      if (property == null) {
        throw new IllegalStateException("can't get <" + key + "> of type byte");
      }
    }
    if (!property.hasByteValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type byte");
    }
    return property.getByteValue();
  }

  public byte getByteValue(String key, byte def) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.BYTE);
      if (property == null) {
        property = Property.createByte(def);
        properties.put(key, property);
      }
    }
    if (!property.hasByteValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type byte");
    }
    return property.getByteValue();
  }

  public int getIntValue(String key) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.INT);
      if (property == null) {
        throw new IllegalStateException("can't get <" + key + "> of type int");
      }
    }
    if (!property.hasIntValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type int");
    }
    return property.getIntValue();
  }

  public int getIntValue(String key, int def) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.INT);
      if (property == null) {
        property = Property.createInt(def);
        properties.put(key, property);
      }
    }
    if (!property.hasIntValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type int");
    }
    return property.getIntValue();
  }

  public long getLongValue(String key) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.LONG);
      if (property == null) {
        throw new IllegalStateException("can't get <" + key + "> of type long");
      }
    }
    if (!property.hasLongValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type long");
    }
    return property.getLongValue();
  }

  public long getLongValue(String key, long def) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.LONG);
      if (property == null) {
        property = Property.createLong(def);
        properties.put(key, property);
      }
    }
    if (!property.hasLongValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type long");
    }
    return property.getLongValue();
  }

  public String getStringValue(String key) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.STRING);
      if (property == null) {
        throw new IllegalStateException("can't get <" + key + "> of type String");
      }
    }
    if (!property.hasStringValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type String");
    }
    return property.getStringValue();
  }

  public String getStringValue(String key, String def) {
    Property property = properties.get(key);
    if (property == null) {
      property = readProperty(key, Type.STRING);
      if (property == null) {
        property = Property.createString(def);
        properties.put(key, property);
      }
    }
    if (!property.hasStringValue()) {
      throw new IllegalStateException("can't get <" + key + "> of type String");
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

  private Property readProperty(String key, Type type) {
    Property property = properties.get(key);

    if (property == null) {
      lock.readLock().lock();
      try {
        PropertiesStorage st = storage;
        switch (type) {
          case BOOLEAN:
            property = Property.createBoolean(st.readBoolean(key));
            break;
          case BYTE:
            property = Property.createByte(st.readByte(key));
            break;
          case INT:
            property = Property.createInt(st.readInt(key));
            break;
          case LONG:
            property = Property.createLong(st.readLong(key));
            break;
          case STRING:
            property = Property.createString(st.readString(key));
            break;
          default:
            throw new IllegalArgumentException("type <" + type + "> is not supported");
        }
      } catch (PropertyException ex) {
        if (PropertyException.ErrorCode.PROPERTY_NOT_FOUND.equals(ex.getErrorCode())) {
          return null;
        }
        throw new IllegalStateException(ex);
      } finally {
        lock.readLock().unlock();
      }

      properties.put(key, property);
    }

    return property;
  }
}
