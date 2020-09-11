/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.javacvsImpl.io.LineReader;
import com.intellij.openapi.vfs.CharsetToolkit;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotEquals;

/**
 * Adapted from ReadLineTest
 */
public class LineReaderTest extends TestCase {
  public void testReadLine() throws IOException {
    doTest("1\n2\n", new String[]{"1", "2", ""}, 2);
    doTest("1\n2", new String[]{"1", "2"}, 2);

    doTest("aa\r\r\nbb\r\r\n", new String[]{"aa", "", "bb", "", ""}, 3);

    doTest("a\r\rb", new String[]{"a", "", "b"}, 2);
    doTest("", new String[]{""}, 2);
    doTest("\r\r\n", new String[]{"", "", ""}, 4);

    doTest("aa\r\r\naa\r\r\nbb\rcc\r\rmm\nuu\r\nee\n\rqq",
           new String[]{"aa", "", "aa", "", "bb", "cc", "", "mm", "uu", "ee", "qq"}, 2);
  }

  public void testEncodingUTF8() throws IOException {
    final String encoding = CharsetToolkit.UTF8;
    final String a = new String(new byte[]{(byte)208, (byte)176}, StandardCharsets.UTF_8);
    final String b = new String(new byte[]{(byte)208, (byte)177}, StandardCharsets.UTF_8);
    final String[] expected = new String[]{a, b};
    assertNotEquals(expected[0], expected[1]);

    doTest(new byte[]{
      (byte)208, (byte)176, (byte)10, (byte)208, (byte)177
    }, expected, encoding, 100);
  }

  private static void doTest(String content, String[] expected, int bufferSize) throws IOException {
    doTest(content.getBytes(StandardCharsets.UTF_8), expected, System.getProperty("file.encoding"), bufferSize);
  }

  private static void doTest(byte[] content, String[] expected, String enc, int bufferSize) throws IOException {
    final LineReader reader = new LineReader(new ByteArrayInputStream(content), Integer.MAX_VALUE, bufferSize);
    final List<byte[]> lines = new ArrayList<>();
    for (byte[] line = reader.readLine(); line != null; line = reader.readLine()) {
      lines.add(line);
    }
    assertEquals(expected.length, lines.size());
    for (int i = 0; i < expected.length; i++) {
      final String actual = new String(lines.get(i), enc);
      assertEquals(expected[i], actual);
    }
  }

  public void testLineBreakOnBufferBreak() throws IOException {
    doTest("aaa\r\rbbb\ra\rc", new String[]{"aaa", "", "bbb", "a", "c"}, 5);
    doTest("a\r\rb\r\r", new String[]{"a", "", "b", "", ""}, 2);
  }
}
