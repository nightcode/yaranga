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

package org.nightcode.api;

import org.nightcode.net.SslContextConfig;

/**
 * API gateway config.
 */
public final class ApiGwConfig {

  public static final class Builder {
    private String address;

    private String           name       = "ApiGw";
    private SslContextConfig sslContext = SslContextConfig.builder().build();

    private Builder() {
    }

    public ApiGwConfig build() {
      return new ApiGwConfig(this);
    }

    public Builder address(String val) {
      address = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder sslContext(SslContextConfig val) {
      sslContext = val;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private String           name;
  private String           address;
  private SslContextConfig sslContext;

  public ApiGwConfig() {
    this(builder());
  }

  private ApiGwConfig(Builder builder) {
    name       = builder.name;
    address    = builder.address;
    sslContext = builder.sslContext;
  }

  public String address() {
    return address;
  }

  public String name() {
    return name;
  }

  public SslContextConfig sslContext() {
    return sslContext;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSslContext(SslContextConfig sslContext) {
    this.sslContext = sslContext;
  }
}
