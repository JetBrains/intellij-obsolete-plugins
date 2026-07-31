package com.intellij.gwt.inspections;

import com.intellij.codeInsight.daemon.impl.analysis.XmlPathReferenceInspection;
import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.references.GwtCodeInsightTestCase;

import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_6;

@GwtTestOptions(version = VERSION_2_6)
public class GwtReferencesTest extends GwtCodeInsightTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    allowTreeAccessToGwtJreEmulationLibrary();
  }

  public void testValidReferenceToOtherRoot() {
    myCodeInsightFixture.enableInspections(XmlPathReferenceInspection.class);
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "resources/pkg/shared/myStyles.css",
                                                  "src/pkg/client/MyComponent.java", "src/pkg/client/MyComponent.ui.xml");
  }

  public void testInvalidReferenceToOtherRoot() {
    myCodeInsightFixture.enableInspections(XmlPathReferenceInspection.class);
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true,
                                                  "src/pkg/client/MyInvalidComponent.java", "src/pkg/client/MyInvalidComponent.ui.xml");
  }

  public void testTagAttributeReference() {
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true,
                                                  "src/pkg/client/MyWidget.java", "src/pkg/client/MyWidget.ui.xml");
  }

  public void testLocalStyleReference() {
    myCodeInsightFixture.enableInspections(UiXmlUnresolvedReferencesInspection.class);
    myCodeInsightFixture.testHighlighting("src/pkg/client/LocalStyleReference.ui.xml");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/references";
  }
}
