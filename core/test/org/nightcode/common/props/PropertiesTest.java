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

package org.nightcode.common.props;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for {@link PropertiesTest}.
 */
public class PropertiesTest {

  @Test public void testReadProperty() {
    Properties properties = Properties.instance();
    properties.setPropertiesStorage(SystemPropertiesStorage.INSTANCE);

    String key = UUID.randomUUID().toString();
    assertNull(System.getProperty(key));
    assertNull(System.getenv(key));

    try {
      properties.getString(key);
      fail("MUST throw PropertyException");
    } catch (IllegalStateException ex) {
      assertEquals("org.nightcode.common.props.PropertyNotFoundException: unable to read property '"
          + key + "' of type STRING", ex.getMessage());
    }

    String targetString = properties.getString(key + "def", "DEFAULT");
    assertEquals("DEFAULT", targetString);

    System.setProperty(key, "bla-bla");
    targetString = properties.getString(key);
    assertEquals("bla-bla", targetString);

    String path = System.getenv("PATH");
    targetString = properties.getString("PATH");
    assertEquals(path, targetString);

    System.setProperty(key + "-boolean", "true");
    assertTrue(properties.getBoolean(key + "-boolean"));

    System.setProperty(key + "-byte", "7");
    assertEquals((byte) 7, properties.getByte(key + "-byte"));

    System.setProperty(key + "-int", "65536");
    assertEquals(65536, properties.getInt(key + "-int"));

    System.setProperty(key + "-long", "6553600000");
    assertEquals(6553600000L, properties.getLong(key + "-long"));
  }

  @Test public void testMapStorage() {
    Map<String, Object> map = new HashMap<>();
    map.put("boolean", Boolean.TRUE);
    map.put("byte", Byte.MAX_VALUE);
    map.put("int", Integer.MAX_VALUE);
    map.put("long", Long.MAX_VALUE);
    map.put("string", "STRING");
    map.put("collection", Collections.singleton("COLLECTION"));

    Properties properties = Properties.instance();
    properties.setPropertiesStorage(new PropertiesMapStorage(map));

    assertEquals(Boolean.TRUE, properties.getBoolean("boolean"));
    assertEquals(Byte.MAX_VALUE, properties.getByte("byte"));
    assertEquals(Integer.MAX_VALUE, properties.getInt("int"));
    assertEquals(Long.MAX_VALUE, properties.getLong("long"));
    assertEquals("STRING", properties.getString("string"));
    assertEquals(Collections.singleton("COLLECTION"), properties.getCollection("collection", String.class));
  }
}
