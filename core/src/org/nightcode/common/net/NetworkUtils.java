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

package org.nightcode.common.net;

import org.nightcode.common.annotations.Beta;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Network helper class.
 */
@Beta
public final class NetworkUtils {

  /**
   * IP range holder.
   */
  public static final class IpAddressRange {
    private final InetAddress firstAddress;
    private final InetAddress lastAddress;
    private final int subnetBits;

    public static IpAddressRange of(InetAddress firstAddress, InetAddress lastAddress, int subnetBits) {
      return new IpAddressRange(firstAddress, lastAddress, subnetBits);
    }

    private IpAddressRange(InetAddress firstAddress, InetAddress lastAddress, int subnetBits) {
      this.firstAddress = firstAddress;
      this.lastAddress = lastAddress;
      this.subnetBits = subnetBits;
    }

    public InetAddress firstAddress() {
      return firstAddress;
    }

    public InetAddress lastAddress() {
      return lastAddress;
    }

    public int subnetBits() {
      return subnetBits;
    }

    @Override public String toString() {
      return "IpAddressRange{"
          + "firstAddress=" + firstAddress
          + ", lastAddress=" + lastAddress
          + ", subnetBits=" + subnetBits
          + '}';
    }
  }

  private static final Pattern CIDR_IP_V4_PATTERN
      = Pattern.compile("^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\\.(?!$)|/(3[0-2]|[1-2][0-9]|[1-9]){1}$)){4}");

  /**
   * Returns CIDR IPv4 pattern.
   */
  public static Pattern cidrIpV4Pattern() {
    return CIDR_IP_V4_PATTERN;
  }

  /**
   * Converts byte array to IP address.
   */
  public static String byteArrayToIpAddress(byte[] src) {
    Objects.requireNonNull(src, "byte array");
    if (src.length != 4) {
      throw new IllegalArgumentException("unsupported IP address byte array representation");
    }
    return (src[0] & 0xFF) + "." + (src[1] & 0xFF) + '.' + (src[2] & 0xFF) + '.' + (src[3] & 0xFF);
  }

  /**
   * Converts CIDR to IP range.
   */
  public static IpAddressRange cidrToIpAddressRange(String cidr) throws UnknownHostException {
    Objects.requireNonNull(cidr, "CIDR");
    int slashIndex = cidr.indexOf('/');
    if (slashIndex == -1 || cidr.length() - slashIndex > 3) {
      throw new IllegalArgumentException("illegal CIDR value '" + cidr + '\'');
    }
    char[] array = cidr.toCharArray();
    byte[] ipAddress = ipAddressToByteArray(array, 0, slashIndex);

    if (ipAddress != null && ipAddress.length == 4) {
      int ip = byteArrayToInt(ipAddress);
      int subnetBits = getDecimalDigitNumber(array, slashIndex + 1, array.length - slashIndex - 1);
      if (subnetBits < 1 || subnetBits > 32) {
        throw new IllegalArgumentException("illegal CIDR value '" + cidr + '\'');
      }
      int subnetMask = 0xFFFFFFFF << (32 - subnetBits);
      int hostMask = ~subnetMask;

      int networkAddress = ip & subnetMask;
      int broadcastAddress = ip | hostMask;

      return IpAddressRange.of(InetAddress.getByAddress(intToByteArray(networkAddress))
          , InetAddress.getByAddress(intToByteArray(broadcastAddress)), subnetBits);
    }

    throw new IllegalArgumentException("unsupported CIDR value '" + cidr + '\'');
  }

  /**
   * Converts IP address to byte array representation.
   */
  public static byte[] ipAddressToByteArray(final String ipAddress) {
    Objects.requireNonNull(ipAddress, "IP address");
    byte[] result = ipAddressToByteArray(ipAddress.toCharArray(), 0, ipAddress.length());
    if (result == null) {
      throw new IllegalArgumentException("unsupported IP address value '" + ipAddress + '\'');
    }
    return result;
  }

  private static int byteArrayToInt(byte[] src) {
    return (((src[0] & 0xFF) << 24) + ((src[1] & 0xFF) << 16) + ((src[2] & 0xFF) << 8) + ((src[3] & 0xFF) << 0));
  }

  private static int getDecimalDigitNumber(char[] src, int offset, int length) {
    int result = 0;
    int digit;
    for (int i = offset; i < offset + length; i++) {
      if (!Character.isDigit(src[i])) {
        throw new IllegalArgumentException("invalid value '" + new String(src, offset, length) + '\'');
      }
      digit = src[i] & 0x0F;
      result *= 10;
      result += digit;
    }
    return result;
  }

  private static byte[] intToByteArray(int src) {
    byte[] array = new byte[4];
    array[0] = (byte) (src >>> 24);
    array[1] = (byte) (src >>> 16);
    array[2] = (byte) (src >>>  8);
    array[3] = (byte) (src >>>  0);
    return array;
  }

  private static byte[] ipAddressToByteArray(char[] src, int offset, int length) {
    boolean ipV4 = true;
    for (int i = offset; i < offset + length; i++) {
      if (src[i] == ':') {
        ipV4 = false;
        break;
      }
    }
    if (!ipV4) {
      return null;
    }
    return ipV4AddressToByteArray(src, offset, length);
  }

  private static byte[] ipV4AddressToByteArray(char[] src, int offset, int length) {
    int octet;
    int part = 0;
    int position = 0;
    byte[] ipAddress = new byte[4];
    for (int i = offset; i < offset + length; i++) {
      if (src[i] == '.') {
        octet = getDecimalDigitNumber(src, position, i - position);
        if (octet < 0 || octet > 255) {
          throw new IllegalArgumentException("invalid IP address '" + new String(src, offset, length) + '\'');
        }
        ipAddress[part] = (byte) (octet & 0xFF);
        part++;
        position = i + 1;
      }
    }
    if (position < offset + length) {
      octet = getDecimalDigitNumber(src, position, offset + length - position);
      if (octet < 0 || octet > 255) {
        throw new IllegalArgumentException("invalid IP address '" + new String(src, offset, length) + '\'');
      }
      ipAddress[part] = (byte) (octet & 0xFF);
    }
    if (part != 3) {
      throw new IllegalArgumentException("invalid IP address '" + new String(src, offset, length) + '\'');
    }
    return ipAddress;
  }

  private NetworkUtils() {
    // do nothing
  }
}
