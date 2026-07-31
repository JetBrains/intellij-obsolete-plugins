package com.intellij.gwt.jsni;

import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.inspections.GwtJavaScriptReferencesInspection;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;

public class GwtJsniInspectionTest extends GwtCodeInsightTestCase {
  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_5)
  public void testQualifiedReferences() {
    myCodeInsightFixture.enableInspections(GwtJavaScriptReferencesInspection.class);
    myCodeInsightFixture.testHighlighting("src/client/QualifiedReferences.java", "src/client/Util.java");
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
  public void testShortReference() {
    myCodeInsightFixture.allowTreeAccessForAllFiles();
    myCodeInsightFixture.enableInspections(GwtJavaScriptReferencesInspection.class);
    myCodeInsightFixture.testHighlighting("src/client/ShortReferences.java", "src/client/Util.java",
                                          "src/client/pack/NotImported.java");
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
  public void testWildcardMethodReferences() {
    myCodeInsightFixture.allowTreeAccessForAllFiles();
    myCodeInsightFixture.enableInspections(GwtJavaScriptReferencesInspection.class);
    myCodeInsightFixture.testHighlighting("src/client/WildcardMethodReferences.java");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "inspections/referencesInJSNI";
  }
}
