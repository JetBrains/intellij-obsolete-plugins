package com.intellij.gwt.css;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/css/findUsages")
public class GwtCssFindUsagesTest extends GwtCodeInsightTestCase {

  public void testVariableUsages() {
    assertEquals(3, myCodeInsightFixture.testFindUsages(getTestName(true) + ".css").size());
  }

  public void testVariableRename() {
    myCodeInsightFixture.testRename(getTestName(true) + ".css", getTestName(true) + "-after.css", "new_name");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "css/findUsages";
  }
}
