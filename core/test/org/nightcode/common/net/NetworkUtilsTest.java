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

import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

public class NetworkUtilsTest {

  @Test public void testIpAddressToByteArray() {
    Assert.assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}, NetworkUtils.ipAddressToByteArray("0.0.0.0"));
    Assert.assertArrayEquals(new byte[] {(byte) 0x03, (byte) 0x30, (byte) 0x00, (byte) 0x00}, NetworkUtils.ipAddressToByteArray("3.48.0.0"));
    Assert.assertArrayEquals(new byte[] {(byte) 0x03, (byte) 0x3F, (byte) 0xFF, (byte) 0xFF}, NetworkUtils.ipAddressToByteArray("3.63.255.255"));
    Assert.assertArrayEquals(new byte[] {(byte) 0xC0, (byte) 0xA8, (byte) 0x17, (byte) 0x22}, NetworkUtils.ipAddressToByteArray("192.168.23.34"));
    Assert.assertArrayEquals(new byte[] {(byte) 0xC0, (byte) 0xA8, (byte) 0x17, (byte) 0x23}, NetworkUtils.ipAddressToByteArray("192.168.23.35"));
    Assert.assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, NetworkUtils.ipAddressToByteArray("255.255.255.255"));

    try {
      NetworkUtils.ipAddressToByteArray("FEDC:BA98:7654:3210:FEDC:BA98:7654:3210");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals("unsupported IP address value 'FEDC:BA98:7654:3210:FEDC:BA98:7654:3210'", ex.getMessage());
    }
  }

  @Test public void testCidrToIpAddressRange() throws UnknownHostException {
    NetworkUtils.IpAddressRange range = NetworkUtils.cidrToIpAddressRange("0.0.0.0/1");
    Assert.assertEquals(1, range.subnetBits());
    Assert.assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}, range.firstAddress().getAddress());
    Assert.assertArrayEquals(new byte[] {(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, range.lastAddress().getAddress());

    range = NetworkUtils.cidrToIpAddressRange("0.0.0.0/32");
    Assert.assertEquals(32, range.subnetBits());
    Assert.assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}, range.firstAddress().getAddress());
    Assert.assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}, range.lastAddress().getAddress());

    range = NetworkUtils.cidrToIpAddressRange("255.255.255.255/1");
    Assert.assertEquals(1, range.subnetBits());
    Assert.assertArrayEquals(new byte[] {(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00}, range.firstAddress().getAddress());
    Assert.assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, range.lastAddress().getAddress());

    range = NetworkUtils.cidrToIpAddressRange("255.255.255.255/32");
    Assert.assertEquals(32, range.subnetBits());
    Assert.assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, range.firstAddress().getAddress());
    Assert.assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, range.lastAddress().getAddress());

    range = NetworkUtils.cidrToIpAddressRange("3.58.1.97/12");
    Assert.assertEquals(12, range.subnetBits());
    Assert.assertArrayEquals(new byte[] {(byte) 0x03, (byte) 0x30, (byte) 0x00, (byte) 0x00}, range.firstAddress().getAddress());
    Assert.assertArrayEquals(new byte[] {(byte) 0x03, (byte) 0x3F, (byte) 0xFF, (byte) 0xFF}, range.lastAddress().getAddress());

    range = NetworkUtils.cidrToIpAddressRange("10.98.1.64/28");
    Assert.assertEquals(28, range.subnetBits());
    Assert.assertArrayEquals(new byte[] {(byte) 0x0A, (byte) 0x62, (byte) 0x01, (byte) 0x40}, range.firstAddress().getAddress());
    Assert.assertArrayEquals(new byte[] {(byte) 0x0A, (byte) 0x62, (byte) 0x01, (byte) 0x4F}, range.lastAddress().getAddress());

    try {
      NetworkUtils.cidrToIpAddressRange("0.0.0.0/0");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals("illegal CIDR value '0.0.0.0/0'", ex.getMessage());
    }

    try {
      NetworkUtils.cidrToIpAddressRange("0.0.0.0/33");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals("illegal CIDR value '0.0.0.0/33'", ex.getMessage());
    }

    try {
      NetworkUtils.cidrToIpAddressRange("FEDC:BA98:7654:3210:FEDC:BA98:7654:3210/64");
      Assert.fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals("unsupported CIDR value 'FEDC:BA98:7654:3210:FEDC:BA98:7654:3210/64'", ex.getMessage());
    }
  }

  @Test public void testCidrPattern() {
    Assert.assertTrue(NetworkUtils.cidrIpV4Pattern().matcher("0.0.0.0/1").matches());
    Assert.assertTrue(NetworkUtils.cidrIpV4Pattern().matcher("255.255.255.255/32").matches());
    Assert.assertTrue(NetworkUtils.cidrIpV4Pattern().matcher("10.98.1.64/28").matches());
    Assert.assertFalse(NetworkUtils.cidrIpV4Pattern().matcher("0.0.0.0/0").matches());
    Assert.assertFalse(NetworkUtils.cidrIpV4Pattern().matcher("0.0.0.0/33").matches());
    Assert.assertFalse(NetworkUtils.cidrIpV4Pattern().matcher("256.255.255.255/32").matches());
  }

  @Test public void testByteArrayToIpAddress() {
    Assert.assertEquals("0.0.0.0", NetworkUtils.byteArrayToIpAddress(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}));
    Assert.assertEquals("3.48.0.0", NetworkUtils.byteArrayToIpAddress(new byte[] {(byte) 0x03, (byte) 0x30, (byte) 0x00, (byte) 0x00}));
    Assert.assertEquals("3.63.255.255", NetworkUtils.byteArrayToIpAddress(new byte[] {(byte) 0x03, (byte) 0x3F, (byte) 0xFF, (byte) 0xFF}));
    Assert.assertEquals("192.168.23.34", NetworkUtils.byteArrayToIpAddress(new byte[] {(byte) 0xC0, (byte) 0xA8, (byte) 0x17, (byte) 0x22}));
    Assert.assertEquals("192.168.23.35", NetworkUtils.byteArrayToIpAddress(new byte[] {(byte) 0xC0, (byte) 0xA8, (byte) 0x17, (byte) 0x23}));
    Assert.assertEquals("255.255.255.255", NetworkUtils.byteArrayToIpAddress(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
  }
}
