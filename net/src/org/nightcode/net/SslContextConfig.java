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

public class SslContextConfig {

  public static final class Builder {
    private String keystorePath;
    private String keystorePassword;
    private String truststorePath;
    private String truststorePassword;

    private boolean useSsl    = false;
    private boolean mutualSsl = false;

    private Builder() {
    }

    public SslContextConfig build() {
      return new SslContextConfig(this);
    }

    public Builder keystorePath(String val) {
      keystorePath = val;
      return this;
    }

    public Builder keystorePassword(String val) {
      keystorePassword = val;
      return this;
    }

    public Builder mutualSsl(boolean val) {
      mutualSsl = val;
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

  private boolean useSsl;
  private String  keystorePath;
  private String  keystorePassword;
  private String  truststorePath;
  private String  truststorePassword;
  private boolean mutualSsl;

  public SslContextConfig() {
    this(builder());
  }

  private SslContextConfig(Builder builder) {
    useSsl             = builder.useSsl;
    keystorePath       = builder.keystorePath;
    keystorePassword   = builder.keystorePassword;
    truststorePath     = builder.truststorePath;
    truststorePassword = builder.truststorePassword;
    mutualSsl          = builder.mutualSsl;
  }

  public String keystorePath() {
    return keystorePath;
  }

  public String keystorePassword() {
    return keystorePassword;
  }

  public boolean mutualSsl() {
    return mutualSsl;
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

  public void setKeystorePassword(String keystorePassword) {
    this.keystorePassword = keystorePassword;
  }

  public void setKeystorePath(String keystorePath) {
    this.keystorePath = keystorePath;
  }

  public void setMutualSsl(boolean mutualSsl) {
    this.mutualSsl = mutualSsl;
  }

  public void setTruststorePassword(String truststorePassword) {
    this.truststorePassword = truststorePassword;
  }

  public void setTruststorePath(String truststorePath) {
    this.truststorePath = truststorePath;
  }

  public void setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
  }
}
