package com.intellij.gwt.model;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.module.index.GwtHtmlUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.text.CharArrayCharSequence;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GwtHtmlUtilTest extends LightPlatformTestCase {
  @NonNls private static final String TEST_DATA = "index" + File.separator + "html" + File.separator;

  public void testGwt13File() throws Exception {
    doTest("ppp.MyModule");
  }

  public void testGwt14File() throws Exception {
    doTest("ppp.MyModule");
  }

  public void testCommentedTag() throws Exception {
    doTest();
  }

  public void testAttributesWithApostrophes() throws IOException {
    doTest("ppp.MyModule");
  }

  public void testScriptWithoutLanguage() throws Exception {
    doTest("ppp.MyModule");
  }

  public void testScriptWithWrongType() throws Exception {
    doTest();
  }

  public void testNameWithDir() throws Exception {
    doTest("ShortName");
  }

  public void testMetaWithIncorrectName() throws Exception {
    doTest();
  }

  public void testManyWhiteSpaces() throws Exception {
    doTest("ppp.MyModule");
  }

  public void testUnexpectedEndOfFile() throws Exception {
    doTest();
  }

  private void doTest(String... moduleNames) throws IOException {
    String fileName = getTestName(false) + ".html";
    final char[] chars = FileUtil.loadFileText(new File(GwtTestCase.getGwtTestDataPath() + TEST_DATA + fileName));
    Map<String, Void> map = new HashMap<>();
    GwtHtmlUtil.collectGwtModules(new CharArrayCharSequence(chars), map);
    assertSameElements(map.keySet(), moduleNames);
  }
}
