package com.intellij.gwt.inspections;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/highlighting/i18n")
public class GwtI18nInspectionTest extends GwtCodeInsightTestCase {
  public void testPluralFormForMessages() {
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.enableInspections(GwtInconsistentLocalizableInterfaceInspection.class);
    myCodeInsightFixture.testHighlighting("src/client/PluralMessages.properties", "src/client/PluralMessages.java");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/i18n";
  }
}
