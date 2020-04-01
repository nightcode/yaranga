package org.nightcode.common.util.props;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class SystemPropertiesStorageTest {

  @Test public void testReadProperty() throws Exception {
    PropertiesStorage storage = new SystemPropertiesStorage();
    
    String key = UUID.randomUUID().toString();
    Assert.assertNull(System.getProperty(key));
    Assert.assertNull(System.getenv(key));

    try {
      storage.readProperty(key, PropertiesStorage.Type.STRING);
      Assert.fail("MUST throw PropertyException");
    } catch (PropertyException ex) {
      Assert.assertTrue(ex instanceof PropertyNotFoundException);
    }

    System.setProperty(key, "bla-bla");
    Property target = storage.readProperty(key, PropertiesStorage.Type.STRING);
    Assert.assertEquals("bla-bla", target.getStringValue());

    String path = System.getenv("PATH");
    target = storage.readProperty("PATH", PropertiesStorage.Type.STRING);
    Assert.assertEquals(path, target.getStringValue());

    System.setProperty(key + "-boolean", "true");
    Assert.assertEquals(true
        , storage.readProperty(key + "-boolean", PropertiesStorage.Type.BOOLEAN).getBooleanValue());

    System.setProperty(key + "-byte", "7");
    Assert.assertEquals((byte) 7, storage.readProperty(key + "-byte"
        , PropertiesStorage.Type.BYTE).getByteValue());

    System.setProperty(key + "-int", "65536");
    Assert.assertEquals(65536, storage.readProperty(key + "-int"
        , PropertiesStorage.Type.INT).getIntValue());

    System.setProperty(key + "-long", "6553600000");
    Assert.assertEquals(6553600000L, storage.readProperty(key + "-long"
        , PropertiesStorage.Type.LONG).getLongValue());
  }
}
