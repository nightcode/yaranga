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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.nightcode.common.lang.ThrowingSupplier;
import org.nightcode.common.logging.Log;
import org.nightcode.common.props.Properties;
import org.nightcode.common.util.Throwables;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.env.EnvScalarConstructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

import freemarker.template.Configuration;
import freemarker.template.Template;

import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;

/**
 * YAML implementation of ConfigLoader.
 */
public enum YamlConfigLoader implements ConfigLoader {
  INSTANCE;

  private static final Boolean DEFAULT_CONFIG_USE_ENV = true;

  private static Configuration getTemplateConfiguration() {
    Configuration cnf = new Configuration(Configuration.VERSION_2_3_34);
    cnf.setDefaultEncoding("UTF-8");
    cnf.setLocale(Locale.ENGLISH);
    cnf.setTemplateExceptionHandler(RETHROW_HANDLER);
    cnf.setLogTemplateExceptions(true);
    cnf.setWrapUncheckedExceptions(true);
    cnf.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
    cnf.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    return cnf;
  }

  @Override public <T> T loadConfig(Class<T> clazz, InputStream src) {
    Log.info().log(YamlConfigLoader.class, "loading configuration from input stream");
    return loadConfig0(clazz, () -> new InputStreamReader(src));
  }

  @Override public <T> T loadConfig(Class<T> clazz, Reader src) {
    Log.info().log(YamlConfigLoader.class, "loading configuration from reader");
    return loadConfig0(clazz, () -> src);
  }

  @Override public <T> T loadConfig(Class<T> clazz, String src) {
    Log.info().log(YamlConfigLoader.class, "loading configuration from string {}", src);
    return loadConfig0(clazz, () -> new StringReader(src));
  }

  @Override public <T> T loadConfig(Class<T> clazz, ThrowingSupplier<Reader, Exception> supplier) {
    Log.info().log(YamlConfigLoader.class, "loading configuration from ReaderSupplier");
    return loadConfig0(clazz, supplier);
  }

  private <T> T loadConfig0(Class<T> clazz, ThrowingSupplier<Reader, Exception> supplier) {
    try (Reader r = supplier.get()) {
      if (isUseEnv()) {
        Template template = new Template("configFile", r, getTemplateConfiguration());
        Writer   out      = new StringWriter();
        template.process(System.getenv(), out);
        String yaml = out.toString();
        return yaml().loadAs(yaml, clazz);
      } else {
        return yaml().loadAs(r, clazz);
      }
    } catch (Exception ex) {
      throw Throwables.rethrow(ex);
    }
  }

  private boolean isUseEnv() {
    return Properties.instance().getBoolean(ConfigLoader.CONFIG_USE_ENV, DEFAULT_CONFIG_USE_ENV);
  }

  private Yaml yaml() {
    Yaml yaml = new Yaml(new EnvScalarConstructor());
    yaml.setBeanAccess(BeanAccess.FIELD);
    return yaml;
  }
}
