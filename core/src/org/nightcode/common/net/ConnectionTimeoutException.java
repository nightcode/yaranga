package org.nightcode.common.net;

public class ConnectionTimeoutException extends GeneralNetworkException {

  public ConnectionTimeoutException(String message) {
    super(message, null);
  }
}
