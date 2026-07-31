package com.intellij.gwt.uiBinder;

import com.intellij.gwt.references.GwtCodeInsightTestCase;

public class GwtUiBinderCompletionTest extends GwtCodeInsightTestCase {

  public void testWithDiv() {
    doTest("theDiv", "theDivElement", "theElement");
  }

  public void testWithTable() {
    doTest("theTable", "theTableElement", "theElement");
  }

  public void testWithObject() {
    doTest("myDiv", "myTable", "myObject");
  }

  private void doTest(String... variants) {
    myCodeInsightFixture.testCompletionVariants(getTestName(false) + ".java", variants);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "uiBinder/completion";
  }
}
