package com.intellij.gwt.jsni;

import com.intellij.gwt.GwtTestCase;
import com.intellij.psi.formatter.FormatterTestCase;

public class GwtJavaScriptFormatterTest extends FormatterTestCase {
  public void testSimple() throws Exception {
    doTest();
  }

  public void testStatements() throws Exception {
    doTest();
  }

  public void testOneLine() throws Exception {
    doTest();
  }
  
  @Override
  protected String getBasePath() {
    return "/jsni/formatting";
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath();
  }

  @Override
  protected String getFileExtension() {
    return "java";
  }
}
