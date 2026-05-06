package org.nightcode.common.props;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for {@link SystemPropertiesStorageTest}.
 */
public class SystemPropertiesStorageTest {

  @Test public void testReadProperty() throws Exception {
    PropertiesStorage storage = SystemPropertiesStorage.INSTANCE;
    
    String key = UUID.randomUUID().toString();
    assertNull(System.getProperty(key));
    assertNull(System.getenv(key));

    try {
      storage.readProperty(key, PropertiesStorage.Type.STRING);
      fail("MUST throw PropertyException");
    } catch (PropertyException ex) {
      assertInstanceOf(PropertyNotFoundException.class, ex);
    }

    System.setProperty(key, "bla-bla");
    Property target = storage.readProperty(key, PropertiesStorage.Type.STRING);
    assertEquals("bla-bla", target.getStringValue());

    String path = System.getenv("PATH");
    target = storage.readProperty("PATH", PropertiesStorage.Type.STRING);
    assertEquals(path, target.getStringValue());

    System.setProperty(key + "-boolean", "true");
    assertTrue(storage.readProperty(key + "-boolean", PropertiesStorage.Type.BOOLEAN).getBooleanValue());

    System.setProperty(key + "-byte", "7");
    assertEquals((byte) 7, storage.readProperty(key + "-byte", PropertiesStorage.Type.BYTE).getByteValue());

    System.setProperty(key + "-int", "65536");
    assertEquals(65536, storage.readProperty(key + "-int", PropertiesStorage.Type.INT).getIntValue());

    System.setProperty(key + "-long", "6553600000");
    assertEquals(6553600000L, storage.readProperty(key + "-long", PropertiesStorage.Type.LONG).getLongValue());
  }
}
