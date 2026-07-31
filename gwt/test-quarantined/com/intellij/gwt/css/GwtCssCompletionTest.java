package com.intellij.gwt.css;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/css/completion/")
public class GwtCssCompletionTest extends GwtCodeInsightTestCase {
  public void testPropertyNames() {
    myCodeInsightFixture.testCompletion(getTestName(true) + ".css", getTestName(true) + "-after.css");
  }

  public void testGwtSpecificPropertyNames() {
    myCodeInsightFixture.testCompletion(getTestName(true) + ".css", getTestName(true) + "-after.css");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "css/completion";
  }
}
