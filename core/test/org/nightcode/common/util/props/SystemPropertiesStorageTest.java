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
      storage.readString(key);
      Assert.fail("MUST throw PropertyException");
    } catch (PropertyException ex) {
      Assert.assertEquals(PropertyException.ErrorCode.PROPERTY_NOT_FOUND, ex.getErrorCode());
    }

    System.setProperty(key, "bla-bla");
    String target = storage.readString(key);
    Assert.assertEquals("bla-bla", target);

    String path = System.getenv("PATH");
    target = storage.readString("PATH");
    Assert.assertEquals(path, target);

    System.setProperty(key + "-boolean", "true");
    Assert.assertEquals(true, storage.readBoolean(key + "-boolean"));

    System.setProperty(key + "-byte", "7");
    Assert.assertEquals((byte) 7, storage.readByte(key + "-byte"));

    System.setProperty(key + "-int", "65536");
    Assert.assertEquals(65536, storage.readInt(key + "-int"));

    System.setProperty(key + "-long", "6553600000");
    Assert.assertEquals(6553600000L, storage.readLong(key + "-long"));
  }
}
