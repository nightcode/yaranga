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

import org.nightcode.common.annotations.Beta;

/**
 * Property container.
 */
@Beta
public final class Property {

  public static Property createBoolean(boolean value) {
    Property property = new Property();
    property.setBooleanValue(value);
    return property;
  }

  public static Property createByte(byte value) {
    Property property = new Property();
    property.setByteValue(value);
    return property;
  }

  public static Property createInt(int value) {
    Property property = new Property();
    property.setIntValue(value);
    return property;
  }

  public static Property createLong(long value) {
    Property property = new Property();
    property.setLongValue(value);
    return property;
  }

  public static Property createString(String value) {
    Property property = new Property();
    property.setStringValue(value);
    return property;
  }

  private int bitField;

  private boolean booleanValue;
  private byte byteValue;
  private int intValue;
  private long longValue;
  private String stringValue;

  private Property() {
    // do nothing
  }

  boolean getBooleanValue() {
    return booleanValue;
  }

  byte getByteValue() {
    return byteValue;
  }

  int getIntValue() {
    return intValue;
  }

  long getLongValue() {
    return longValue;
  }

  String getStringValue() {
    return stringValue;
  }

  boolean hasBooleanValue() {
    return (bitField & 0x00000001) == 0x00000001;
  }

  boolean hasByteValue() {
    return (bitField & 0x00000002) == 0x00000002;
  }

  boolean hasIntValue() {
    return (bitField & 0x00000004) == 0x00000004;
  }

  boolean hasLongValue() {
    return (bitField & 0x00000008) == 0x00000008;
  }

  boolean hasStringValue() {
    return (bitField & 0x00000010) == 0x00000010;
  }

  private void setBooleanValue(boolean value) {
    bitField |= 0x00000001;
    booleanValue = value;
  }

  private void setByteValue(byte value) {
    bitField |= 0x00000002;
    byteValue = value;
  }

  private void setIntValue(int value) {
    bitField |= 0x00000004;
    intValue = value;
  }

  private void setLongValue(long value) {
    bitField |= 0x00000008;
    longValue = value;
  }

  private void setStringValue(String value) {
    bitField |= 0x00000010;
    stringValue = value;
  }
}
