package com.intellij.gwt.inspections;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;
import com.siyeh.ig.performance.DynamicRegexReplaceableByCompiledPatternInspection;

@TestDataPath("$CONTENT_ROOT/../testData/highlighting/")
public class GwtAllowedClientCodeInspectionsTest extends GwtCodeInsightTestCase {
  public void testDoNotSuggestReplaceByCompiledPattern() {
    myCodeInsightFixture.enableInspections(DynamicRegexReplaceableByCompiledPatternInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "notAllowedApi/client/DynamicRegExp.java");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting";
  }
}
