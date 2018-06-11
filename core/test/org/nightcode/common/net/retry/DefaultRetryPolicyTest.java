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

import org.junit.Assert;
import org.junit.Test;

public class DefaultRetryPolicyTest {

  @Test public void testOnException() {
    RetryPolicy retryPolicy = new DefaultRetryPolicy();

    Throwable retryException = new IOException();
    RetryPolicy.Decision target = retryPolicy.onException(retryException);
    Assert.assertEquals(RetryPolicy.Decision.RETRY, target);

    retryException = new GeneralNetworkException("message", retryException);
    target = retryPolicy.onException(retryException);
    Assert.assertEquals(RetryPolicy.Decision.RETRY, target);

    Throwable rethrowException = new IllegalArgumentException();
    target = retryPolicy.onException(rethrowException);
    Assert.assertEquals(RetryPolicy.Decision.RETHROW, target);

    Throwable retryCauseException = new ExecutionException("message", retryException);
    target = retryPolicy.onException(retryCauseException);
    Assert.assertEquals(RetryPolicy.Decision.RETRY, target);

    retryCauseException = new CompletionException("message", retryException);
    target = retryPolicy.onException(retryCauseException);
    Assert.assertEquals(RetryPolicy.Decision.RETRY, target);
    
    Throwable rethrowCauseException = new ExecutionException("message", rethrowException);
    target = retryPolicy.onException(rethrowCauseException);
    Assert.assertEquals(RetryPolicy.Decision.RETHROW, target);

    rethrowCauseException = new CompletionException("message", rethrowException);
    target = retryPolicy.onException(rethrowCauseException);
    Assert.assertEquals(RetryPolicy.Decision.RETHROW, target);
  }
}
