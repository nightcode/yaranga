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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesTest {

  @Test public void testReadProperty() {
    Properties properties = Properties.instance();
    properties.setPropertiesStorage(SystemPropertiesStorage.INSTANCE);

    String key = UUID.randomUUID().toString();
    Assert.assertNull(System.getProperty(key));
    Assert.assertNull(System.getenv(key));

    try {
      properties.getStringValue(key);
      Assert.fail("MUST throw PropertyException");
    } catch (IllegalStateException ex) {
      Assert.assertEquals("org.nightcode.common.util.props.PropertyNotFoundException: unable to read property '"
          + key + "' of type STRING", ex.getMessage());
    }

    String targetString = properties.getStringValue(key + "def", "DEFAULT");
    Assert.assertEquals("DEFAULT", targetString);

    System.setProperty(key, "bla-bla");
    targetString = properties.getStringValue(key);
    Assert.assertEquals("bla-bla", targetString);

    String path = System.getenv("PATH");
    targetString = properties.getStringValue("PATH");
    Assert.assertEquals(path, targetString);

    System.setProperty(key + "-boolean", "true");
    Assert.assertEquals(true, properties.getBooleanValue(key + "-boolean"));

    System.setProperty(key + "-byte", "7");
    Assert.assertEquals((byte) 7, properties.getByteValue(key + "-byte"));

    System.setProperty(key + "-int", "65536");
    Assert.assertEquals(65536, properties.getIntValue(key + "-int"));

    System.setProperty(key + "-long", "6553600000");
    Assert.assertEquals(6553600000L, properties.getLongValue(key + "-long"));
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

    Assert.assertEquals(Boolean.TRUE, properties.getBooleanValue("boolean"));
    Assert.assertEquals(Byte.MAX_VALUE, properties.getByteValue("byte"));
    Assert.assertEquals(Integer.MAX_VALUE, properties.getIntValue("int"));
    Assert.assertEquals(Long.MAX_VALUE, properties.getLongValue("long"));
    Assert.assertEquals("STRING", properties.getStringValue("string"));
    Assert.assertEquals(Collections.singleton("COLLECTION"), properties.getCollectionValue("collection", String.class));
  }
}
