package com.intellij.gwt.injected;

import com.intellij.codeInsight.generation.AutoIndentLinesHandler;
import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.javascript.GwtJsTestUtil;
import com.intellij.lang.javascript.JSBaseEditorTestCase;
import com.intellij.openapi.command.WriteCommandAction;

public class GwtJsEditorTest extends JSBaseEditorTestCase {
  public void testSmartEnter() {
    GwtJsTestUtil.setUpGwtDialect();
    doSmartEnterTest("SmartEnterStatement", "GwtJavaScript");
  }

  public void testSmartEnterInInjectedCode() {
    doSmartEnterTest("SmartEnterStatement", "java");
  }

  public void testEnter() {
    doEnterTest("Enter", "java");
  }

  public void testAutoIndent() {
    myFixture.configureByFile("/AutoIndent.java");
    WriteCommandAction.runWriteCommandAction(getProject(), () -> new AutoIndentLinesHandler().invoke(getProject(), myFixture.getEditor(), myFixture.getFile()));

    myFixture.checkResultByFile("/AutoIndent_after.java");
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath() + "jsni/editor/";
  }
}
