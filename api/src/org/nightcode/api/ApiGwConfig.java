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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * API gateway config.
 */
public final class ApiGwConfig {

  public static final class Builder {
    private String address;
    private String keystorePath;
    private String keystorePassword;
    private String truststorePath;
    private String truststorePassword;

    private String                 name         = "ApiGw";
    private boolean                useSsl       = false;
    private int                    poolSize     = Runtime.getRuntime().availableProcessors();
    private List<ApiGwInterceptor> interceptors = Collections.emptyList();

    private Builder() {
    }

    public ApiGwConfig build() {
      return new ApiGwConfig(this);
    }

    public Builder address(String val) {
      address = val;
      return this;
    }

    public Builder interceptor(ApiGwInterceptor... val) {
      interceptors = Arrays.asList(val);
      return this;
    }

    public Builder keystorePath(String val) {
      keystorePath = val;
      return this;
    }

    public Builder keystorePassword(String val) {
      keystorePassword = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder poolSize(int val) {
      poolSize = val;
      return this;
    }

    public Builder truststorePath(String val) {
      truststorePath = val;
      return this;
    }

    public Builder truststorePassword(String val) {
      truststorePassword = val;
      return this;
    }

    public Builder useSsl(boolean val) {
      useSsl = val;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final String  name;
  private final String  address;
  private final int     poolSize;
  private final boolean useSsl;
  private final String  keystorePath;
  private final String  keystorePassword;
  private final String  truststorePath;
  private final String  truststorePassword;

  private List<ApiGwInterceptor> interceptors;

  public ApiGwConfig() {
    this(builder());
  }

  private ApiGwConfig(Builder builder) {
    name               = builder.name;
    address            = builder.address;
    poolSize           = builder.poolSize;
    useSsl             = builder.useSsl;
    keystorePath       = builder.keystorePath;
    keystorePassword   = builder.keystorePassword;
    truststorePath     = builder.truststorePath;
    truststorePassword = builder.truststorePassword;
    interceptors       = builder.interceptors;
  }

  public String address() {
    return address;
  }

  public List<ApiGwInterceptor> interceptors() {
    return interceptors;
  }

  public String keystorePath() {
    return keystorePath;
  }

  public String keystorePassword() {
    return keystorePassword;
  }

  public String name() {
    return name;
  }

  public int poolSize() {
    return poolSize;
  }

  public String truststorePath() {
    return truststorePath;
  }

  public String truststorePassword() {
    return truststorePassword;
  }

  public boolean useSsl() {
    return useSsl;
  }

  public ApiGwConfig interceptor(ApiGwInterceptor... val) {
    interceptors = Arrays.asList(val);
    return this;
  }
}
