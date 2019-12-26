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

/**
 * todo.
 */
public class PropertyException extends Exception {

  /**
   * todo.
   */
  public enum ErrorCode {
    PROPERTY_NOT_FOUND,
    STORAGE_IS_NOT_AVAILABLE
  }

  private final ErrorCode errorCode;

  public PropertyException(ErrorCode errorCode) {
    this(errorCode, null, null);
  }

  public PropertyException(ErrorCode errorCode, String message) {
    this(errorCode, message, null);
  }

  public PropertyException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause, true, false);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
