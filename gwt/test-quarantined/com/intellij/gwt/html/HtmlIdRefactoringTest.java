package com.intellij.gwt.html;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/html/refactoring/renameIdInWeb/")
public class HtmlIdRefactoringTest extends GwtCodeInsightTestCase {
  public void testRenameIdInWeb() {
    myCodeInsightFixture.configureByFile("web/MyModule.html");
    myCodeInsightFixture.renameElementAtCaret("renamedSlot");
    myCodeInsightFixture.checkResultByFile("src/client/MyModule.java", "src/client/MyModule_after.java", true);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "html/refactoring/renameIdInWeb";
  }
}
