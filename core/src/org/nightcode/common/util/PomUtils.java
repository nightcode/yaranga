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

package org.nightcode.common.util;

import java.io.InputStream;
import java.util.Properties;

import org.nightcode.common.logging.Log;

/**
 * POM utils.
 */
public enum PomUtils {
  ;

  public static Properties create(String groupId, String artifactId) {
    Properties properties = new Properties();
    try {
      String resourceName = resourceName(groupId, artifactId);
      try (InputStream in = PomUtils.class.getResourceAsStream(resourceName)) {
        if (in != null) {
          properties.load(in);
        }
      }
    } catch (Exception ex) {
      Log.debug().log(PomUtils.class, "failed to load pom properties", ex);
    }
    return properties;
  }

  public static String version(String groupId, String artefactId) {
    return create(groupId, artefactId).getProperty("version", "unknown");
  }

  private static String resourceName(String groupId, String artifactId) {
    return "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
  }
}
