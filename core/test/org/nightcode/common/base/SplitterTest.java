/*
 * Copyright (C) 2008 The NightCode Open Source Project
 *
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

package org.nightcode.common.base;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link Splitter}.
 */
public class SplitterTest {
  
  @Test public void splitStringDefault() {
    Splitter splitter = Splitter.on("&");
    
    final String str = "a=b&c=\"d&e=\"f\"&d=&=e";
    Map<String, String> actual = splitter.split(str);
    
    assertEquals("b", actual.get("a"));
    assertEquals("\"d", actual.get("c"));
    assertEquals("\"f\"", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }
  
  @Test public void splitString() {
    Splitter splitter = Splitter.on("&").withKeyValueSeparator("=");
    
    final String str = "a=b&c=\"d&e=\"f\"&d=&=e";
    Map<String, String> actual = splitter.split(str);
    
    assertEquals("b", actual.get("a"));
    assertEquals("\"d", actual.get("c"));
    assertEquals("\"f\"", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringDefaultWithTrim() {
    Splitter splitter = Splitter.on("&").trimValues('\"');

    final String str = "a=b&c=\"d&e=\"\"f\"&d=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringWithKeyTrim() {
    Splitter splitter 
        = Splitter.on("&").withKeyValueSeparator("=").trimKeys('\"');

    final String str = "\"a\"=b&c\"=\"d&e=\"f\"&\"d=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("\"d", actual.get("c"));
    assertEquals("\"f\"", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringWithComplexKeyTrim() {
    Splitter splitter 
        = Splitter.on("&").withKeyValueSeparator("=").trimKeys(new char[] {'[', ']'});

    final String str = "[a=b&[c]=[d&e=[f]&d]=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("[d", actual.get("c"));
    assertEquals("[f]", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringWithComplexKeyValueTrim() {
    Splitter splitter 
        = Splitter.on("&").withKeyValueSeparator("=")
        .trimKeys(new char[] {'[',']'}).trimValues(new char[] {'[',']'});

    final String str = "[a=b&[c]=[d&e=[f]&d]=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringWithComplexTrim() {
    Splitter splitter = Splitter.on("&").withKeyValueSeparator("=").trim(new char[] {'[',']'});

    final String str = "[a=b&[c]=[d&e=[f]&d]=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringWithValueTrim() {
    Splitter splitter 
        = Splitter.on("&").withKeyValueSeparator("=").trimValues('\"');

    final String str = "a=b&c=\"d&e=\"f\"&d=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringWithComplexValueTrim() {
    Splitter splitter 
        = Splitter.on("&").withKeyValueSeparator("=").trimValues(new char[] {'[',']'});

    final String str = "a=b&c=[d&e=[f]&d=&=e";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("d"));
    assertNull(actual.get(""));
    assertEquals(4, actual.size());
  }

  @Test public void splitStringComplexKeyValueSeparator() {
    Splitter splitter
        = Splitter.on("&").withKeyValueSeparator(":=").trimValues(new char[] {'[',']'});

    final String str = "a:=b&c:=[d&e:=[f]&g:=&:=h&i:j&k:::l:=m";
    Map<String, String> actual = splitter.split(str);

    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("g"));
    assertNull(actual.get("i:"));
    assertEquals("m", actual.get("k:::l"));
    assertNull(actual.get(""));
    assertEquals(6, actual.size());
  }
  
  @Test public void splitStringComplexSeparator() {
    Splitter splitter 
        = Splitter.on("&=").withKeyValueSeparator(":=").trimValues(new char[] {'[',']'});
    
    final String str = "a:=b&=c:=[d&=e:=[f]&=&=&=g:=&=:=h&=i:&&=";
    Map<String, String> actual = splitter.split(str);
        
    assertEquals("b", actual.get("a"));
    assertEquals("d", actual.get("c"));
    assertEquals("f", actual.get("e"));
    assertEquals("", actual.get("g"));
    assertNull(actual.get("i:&"));
    assertNull(actual.get(""));
    assertEquals(5, actual.size());
  }
  
  @Test public void splitNpe() {
    Splitter splitter = Splitter.on("&");
    
    try {
      splitter.split(null);
    } catch(NullPointerException ex) {
      assertTrue(ex.getMessage().contains("source"));
      return;
    }
    fail();
  }
}
