package com.intellij.gwt.jsni;

import com.intellij.gwt.inspections.GwtJavaFromJSMethodCallsInspection;
import com.intellij.gwt.references.GwtCodeInsightTestCase;

public class GwtJavaFromJSMethodCallsInspectionTest extends GwtCodeInsightTestCase {
  public void testMethodCalls() {
    myCodeInsightFixture.allowTreeAccessForAllFiles();
    myCodeInsightFixture.enableInspections(GwtJavaFromJSMethodCallsInspection.class);
    myCodeInsightFixture.testHighlighting("src/client/MyClass.java");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "inspections/javaFromJSMethodCalls";
  }
}
