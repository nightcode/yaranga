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

package org.nightcode.net;

import java.util.Objects;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.ssl.SslContext;
import org.nightcode.common.pool.Session;

public class SslFactory implements BootstrapFactory {

  private final BootstrapFactory target;
  private final SslContext       sslContext;

  public SslFactory(BootstrapFactory target, SslContext sslContext) {
    this.target     = Objects.requireNonNull(target, "delegate");
    this.sslContext = Objects.requireNonNull(sslContext, "sslContext");
  }

  @Override public Bootstrap create(PipeContext<?, ? extends Session<?>> context) {
    return target.create(context);
  }

  @Override public SslContext sslContext() {
    return sslContext;
  }
}
