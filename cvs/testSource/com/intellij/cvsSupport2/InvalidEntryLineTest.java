package com.intellij.cvsSupport2;

import org.junit.Test;
import org.netbeans.lib.cvsclient.admin.Entries;
import org.netbeans.lib.cvsclient.admin.Entry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * author: lesya
 */
public class InvalidEntryLineTest {
  @Test
  public void test() throws IOException {
    Entries entries = new Entries();
    entries.read(new InputStreamReader(new ByteArrayInputStream("\\".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    assertEquals(1, entries.getEntries().size());
    Entry first = entries.getEntries().iterator().next();
    assertNull(first.getFileName());
  }
}
