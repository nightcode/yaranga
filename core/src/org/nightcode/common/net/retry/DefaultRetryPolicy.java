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

package org.nightcode.common.net.retry;

import org.nightcode.common.net.GeneralNetworkException;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * The default implementation of a retry policy.
 */
public class DefaultRetryPolicy implements RetryPolicy {

  @Override public Decision onException(Throwable cause) {
    if ((cause instanceof ExecutionException) || (cause instanceof CompletionException)) {
      if (cause.getCause() != null) {
        cause = cause.getCause();
      }
    }
    if (cause instanceof IOException || cause instanceof GeneralNetworkException) {
      return Decision.RETRY;
    }
    return Decision.RETHROW;
  }
}
