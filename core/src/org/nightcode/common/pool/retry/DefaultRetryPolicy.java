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

package org.nightcode.common.pool.retry;

/**
 * The default implementation of a retry policy.
 */
public enum DefaultRetryPolicy implements RetryPolicy {

  INSTANCE;

  @Override public Decision onRequestError(Throwable cause) {
    return Decision.TRY_NEXT;
  }

  @Override public Decision onResponseError(Throwable cause) {
    return Decision.RETHROW;
  }

  @Override public Decision onUnavailable() {
    return Decision.TRY_NEXT;
  }
}
