package com.intellij.gwt.references;

import com.intellij.codeInspection.htmlInspections.RequiredAttributesInspection;
import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.inspections.GwtUiFieldErrorsInspection;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.testFramework.TestDataPath;
import com.intellij.xml.util.CheckXmlFileWithXercesValidatorInspection;

@GwtTestOptions(version = GwtVersionImpl.VERSION_2_6)
@TestDataPath("$CONTENT_ROOT/../testData/highlighting/uiBinder/")
public class GwtUiBinderHighlightingTest extends GwtCodeInsightTestCase {

  public void testUiXmlFile() {
    myCodeInsightFixture.configureByFile("xxx/client/MyComponent.ui.xml");
    checkXmlHighlighting();
  }

  public void testUiFactory() {
    myCodeInsightFixture.configureByFile("xxx/client/factory/MyPanel.ui.xml");
    checkXmlHighlighting();
  }

  public void testProvidedUiField() {
    myCodeInsightFixture.enableInspections(GwtUiFieldErrorsInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "xxx/client/MyCompWithRes.ui.xml", "xxx/client/MyCompWithRes.java");
  }

  public void testEntDoctype() {
    myCodeInsightFixture.configureByFile("xxx/client/EntDoctype.ui.xml");
    checkXmlHighlighting();
  }

  public void testUiChildTag() {
    myCodeInsightFixture.configureByFile("xxx/client/UiChildTag.ui.xml");
    checkXmlHighlighting();
  }

  public void testUiChildTagInSuperClass() {
    myCodeInsightFixture.configureByFile("xxx/client/UiChildTagInSuperClass.ui.xml");
    checkXmlHighlighting();
  }

  public void testUiChildWithParam() {
    myCodeInsightFixture.configureByFile("xxx/client/UiChildWithParam.ui.xml");
    checkXmlHighlighting();
  }

  public void testDebugIdAttribute() {
    myCodeInsightFixture.configureByFile("xxx/client/DebugIdComp.ui.xml");
    checkXmlHighlighting();
  }

  public void testIntPairAttribute() {
    myCodeInsightFixture.configureByFiles("xxx/client/IntPairComp.ui.xml", "xxx/client/MyWidget.java");
    checkXmlHighlighting();
  }

  public void testLazyDomElement() {
    myCodeInsightFixture.enableInspections(GwtUiFieldErrorsInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "xxx/client/LazyDomElementComponent.ui.xml", "xxx/client/LazyDomElementComponent.java");
  }

  public void testWidgetWithParameter() {
    myCodeInsightFixture.enableInspections(RequiredAttributesInspection.class);
    myCodeInsightFixture.configureByFile("xxx/client/param/WidgetWithParamPanel.ui.xml");
    checkXmlHighlighting();
  }

  private void checkXmlHighlighting() {
    myCodeInsightFixture.enableInspections(CheckXmlFileWithXercesValidatorInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.checkHighlighting();
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/uiBinder";
  }
}
