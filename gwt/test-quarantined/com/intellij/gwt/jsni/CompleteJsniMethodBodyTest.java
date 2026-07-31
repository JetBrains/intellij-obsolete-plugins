package com.intellij.gwt.jsni;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/jsni/completion/java/")
public class CompleteJsniMethodBodyTest extends GwtCodeInsightTestCase {
  public void testCompleteBody() {
    doTest();
  }

  public void testNonNativeMethod() {
    doTest();
  }

  public void testCompleteBodyAfterSemicolon() {
    doTest();
  }

  public void testCompleteBodyAfterComment() {
    doTest();
  }

  public void testCompleteBodyAfterComment2() {
    doTest();
  }

  private void doTest() {
    final String testName = getTestName(false);
    myCodeInsightFixture.configureByFile("client/" + testName + ".java");
    myCodeInsightFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT);
    myCodeInsightFixture.checkResultByFile("client/" + testName + "_after.java");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "jsni/completion/java";
  }
}
