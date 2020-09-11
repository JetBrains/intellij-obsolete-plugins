package com.intellij.cvsSupport2;

import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.text.LineReader;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertNotEquals;

/**
 * author: lesya
 */
public class ReadLineTest extends TestCase {
  public void testReadLine() throws IOException {
    doTest("1\n2\n", new String[]{"1", "2", ""});
    doTest("1\n2", new String[]{"1", "2"});

    doTest("aa\r\r\naa\r\r\n", new String[]{"aa", "aa", ""});

    doTest("a\r\rb", new String[]{"a", "", "b"});
    doTest("", new String[]{""});
    doTest("\r\r\n", new String[]{"", ""});

    doTest("aa\r\r\naa\r\r\nbb\rcc\r\rmm\nuu\r\nee\n\rqq",
        new String[]{"aa", "aa", "bb", "cc", "", "mm", "uu", "ee", "", "qq"});
  }


  public void testEncodingWindows11251() throws IOException {
    String a = new String(new byte[]{(byte)228}, StandardCharsets.UTF_8);
    String b = new String(new byte[]{(byte)235}, StandardCharsets.UTF_8);
    String[] expected = new String[]{a, b};
    assertNotEquals(expected[0], expected[1]);
    doTest(a + "\n" + b, expected);
  }

  public void testEncodingUTF8() throws IOException {
    String encoding = CharsetToolkit.UTF8;
    String a = new String(new byte[]{(byte)208, (byte)176}, StandardCharsets.UTF_8);
    String b = new String(new byte[]{(byte)208, (byte)177}, StandardCharsets.UTF_8);
    String[] expected = new String[]{a, b};
    assertNotEquals(expected[0], expected[1]);

    doTest(new byte[]{
      (byte)208, (byte)176, (byte)10, (byte)208, (byte)177
    }, expected, encoding);
  }

  private static void doTest(String content, String[] expected) throws IOException {
    doTest(content.getBytes(StandardCharsets.UTF_8), expected, System.getProperty("file.encoding"));
  }

  private static void doTest(byte[] content, String[] expected, String enc) throws IOException {
    LineReader reader = new LineReader(new ByteArrayInputStream(content));
    List lines = reader.readLines();
    assertEquals(expected.length, lines.size());
    for (int i = 0; i < expected.length; i++) {
      String s = expected[i];
      assertEquals(s, new String((byte[]) lines.get(i), enc));
    }
  }

}
