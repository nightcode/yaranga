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

package org.nightcode.common.net.http;

import java.io.IOException;
import java.util.UUID;

import org.nightcode.common.logging.Log;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public enum SwallowStackTraceFilter implements Filter {
  INSTANCE;

  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    try {
      chain.doFilter(request, response);
    } catch (Exception ex) {
      UUID uuid = UUID.randomUUID();
      Log.warn().log(SwallowStackTraceFilter.class, ex, "unexpected exception during request processing #{}", uuid);
      try {
        response.getOutputStream().println("error #" + uuid);
      } catch (IOException io) {
        Log.warn().log(SwallowStackTraceFilter.class, "unable to send error message", io);
      }
    }
  }
}
