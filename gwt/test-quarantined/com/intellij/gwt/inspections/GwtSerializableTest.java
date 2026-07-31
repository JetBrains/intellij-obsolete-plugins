package com.intellij.gwt.inspections;

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase;
import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;

public class GwtSerializableTest extends GwtCodeInsightTestCase {

  public void testWithDefaultConstructor() {
    doTest();
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_8)
  public void testWithPrivateDefaultConstructor28() {
    doTest();
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_1_4)
  public void testWithPrivateDefaultConstructor14() {
    doTest();
  }

  private void doTest() {
    myCodeInsightFixture.configureByFile("client/" + getTestName(false) + ".java");
    myCodeInsightFixture.enableInspections(new UnusedDeclarationInspectionBase(true));
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.checkHighlighting(true, false, true);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/serializable";
  }
}
