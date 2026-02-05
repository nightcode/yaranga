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

package org.nightcode.common.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.nightcode.common.lang.ThrowingSupplier;
import org.nightcode.common.props.Properties;
import org.nightcode.common.props.SystemPropertiesStorage;

import org.junit.Assert;
import org.junit.Test;

import static org.nightcode.common.config.ConfigLoader.CONFIG_USE_ENV;

/**
 * Unit test for {@link YamlConfigLoader}.
 */
public class YamlConfigLoaderTest {

  private static class TestConfig {
    String variable;
  }

  private static final String JSON = "variable: '[=PATH!\"empty\"]'";

  @Test public void testEnv() {
    TestConfig config;

    System.setProperty(CONFIG_USE_ENV, "true");
    config = YamlConfigLoader.INSTANCE.loadConfig(TestConfig.class, JSON);
    Assert.assertNotEquals("[=PATH!\"empty\"]", config.variable);

    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty(CONFIG_USE_ENV, "false");
    config = YamlConfigLoader.INSTANCE.loadConfig(TestConfig.class, JSON);
    Assert.assertEquals("[=PATH!\"empty\"]", config.variable);
  }

  @Test public void testLoadConfigInputStream() {
    TestConfig  config;
    InputStream src = new ByteArrayInputStream(JSON.getBytes(StandardCharsets.UTF_8));

    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty(CONFIG_USE_ENV, "false");
    config = ConfigLoader.yaml().loadConfig(TestConfig.class, src);
    Assert.assertEquals("[=PATH!\"empty\"]", config.variable);
  }

  @Test public void testLoadConfigReader() {
    TestConfig config;
    Reader     src = new StringReader(JSON);

    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty(CONFIG_USE_ENV, "false");
    config = ConfigLoader.yaml().loadConfig(TestConfig.class, src);
    Assert.assertEquals("[=PATH!\"empty\"]", config.variable);
  }

  @Test public void testLoadConfigReaderSupplier() {
    TestConfig                          config;
    ThrowingSupplier<Reader, Exception> supplier = () -> new StringReader(JSON);

    Properties.instance().setPropertiesStorage(SystemPropertiesStorage.INSTANCE);
    System.setProperty(CONFIG_USE_ENV, "false");
    config = ConfigLoader.yaml().loadConfig(TestConfig.class, supplier);
    Assert.assertEquals("[=PATH!\"empty\"]", config.variable);
  }
}
