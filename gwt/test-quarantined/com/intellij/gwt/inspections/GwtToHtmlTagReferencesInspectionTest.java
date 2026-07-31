package com.intellij.gwt.inspections;

import com.intellij.gwt.references.GwtCodeInsightTestCase;

public class GwtToHtmlTagReferencesInspectionTest extends GwtCodeInsightTestCase {

  public void testQuickFix() {
    myCodeInsightFixture.enableInspections(GwtToHtmlTagReferencesInspection.class);

    myCodeInsightFixture.openFileInEditor(myCodeInsightFixture.findFileInTempDir("src/client/MyModule.java"));

    myCodeInsightFixture.launchAction(assertOneElement(myCodeInsightFixture.getAllQuickFixes("src/client/MyModule.java")));

    myCodeInsightFixture.checkResultByFile("src/client/MyModule_after.java");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "intentions/gwtToHtmlTagReference";
  }
}
