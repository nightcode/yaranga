package org.nightcode.common.net.im;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ContentTypeParser {

  private static final int SIG_TOKEN = 1;
  private static final int[] TOKEN_SIGNALS = new int[0xFF];

  static {
    Arrays.fill(TOKEN_SIGNALS, 0);
    TOKEN_SIGNALS[0x2D] = SIG_TOKEN; // -
    for (int i = 0x30; i <= 0x39; i++) { // digits
      TOKEN_SIGNALS[i] = SIG_TOKEN;
    }
    for (int i = 0x41; i <= 0x5A; i++) { // A-Z
      TOKEN_SIGNALS[i] = SIG_TOKEN;
    }
    for (int i = 0x61; i <= 0x7A; i++) { // a-z
      TOKEN_SIGNALS[i] = SIG_TOKEN;
    }
  }

  private static boolean isTokenChar(char c) {
    return TOKEN_SIGNALS[c] == SIG_TOKEN;
  }

  private static int readToken(char[] src, int i) {
    for (int length = src.length; i < length && isTokenChar(src[i]); i++) {
      // do nothing;
    }
    return i;
  }

  private static int skipWhiteSpace(char[] src, int i) {
    for (int length = src.length; i < length && Character.isWhitespace(src[i]); i++) {
      // do nothing
    }
    return i;
  }

  public ContentType parse(String src) {
    char[] array = src.toCharArray();
    int length = array.length;

    int p = skipWhiteSpace(array, 0);
    if (p >= length) {
      throw new IllegalArgumentException("illegal Content-Type: " + src);
    }

    int offset = p;
    p = readToken(array, p);
    if (p >= length || array[p] != '/') {
      throw new IllegalArgumentException("illegal Content-Type: " + src);
    }

    String type = new String(array, offset, p - offset).toLowerCase();

    offset = ++p;
    p = readToken(array, p);
    if (offset == p) {
      throw new IllegalArgumentException("illegal Content-Type: " + src);
    }

    String subType = new String(array, offset, p - offset).toLowerCase();

    p = skipWhiteSpace(array, p);
    if (p >= length) {
      return new ContentType(type, subType);
    }

    String attribute;
    String value;
    StringBuilder sb = new StringBuilder();
    Map<String, String> parameters = new HashMap<>();
    while (p < length && array[p] == ';') {
      p = skipWhiteSpace(array, ++p);
      offset = p;
      p = readToken(array, p);
      attribute = new String(array, offset, p - offset).toLowerCase();
      p = skipWhiteSpace(array, p);
      if (p >= length || array[p] != '=') {
        throw new IllegalArgumentException("illegal Content-Type: " + src);
      }

      p = skipWhiteSpace(array, ++p);
      if (p >= length) {
        throw new IllegalArgumentException("illegal Content-Type: " + src);
      }

      if (array[p] == '"') {
        p++;
        if (p >= length) {
          throw new IllegalArgumentException("illegal Content-Type: " + src);
        }
        while (p < length) {
          char c = array[p];
          if (c == '\\') {
            sb.append(array[p + 1]);
            p += 2;
          } else if (c == '"') {
            break;
          } else {
            sb.append(array[p]);
            p++;
          }
        }
        if (p >= length || array[p] != '"') {
          throw new IllegalArgumentException("illegal Content-Type: " + src);
        }
        value = sb.toString();
        sb.setLength(0);
        p++;
      } else {
        offset = p;
        p = readToken(array, p);
        value = new String(array, offset, p - offset);
      }
      parameters.put(attribute, value);
      p = skipWhiteSpace(array, p);
    }

    if (p < length) {
      throw new IllegalArgumentException("illegal Content-Type: " + src);
    }

    return new ContentType(type, subType, parameters);
  }
}
