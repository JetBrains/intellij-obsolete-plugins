package com.intellij.gwt.references;

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/highlighting/imageResources/")
public class ImageResourceHighlightingTest extends GwtCodeInsightTestCase {
  public void testImageUsedInCss() {
    myCodeInsightFixture.configureByFile("client/MyResources.java");
    myCodeInsightFixture.enableInspections(new UnusedDeclarationInspectionBase(true));
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.checkHighlighting(true, false, true);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/imageResources";
  }
}
