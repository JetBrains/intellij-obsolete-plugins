package com.intellij.gwt.inspections;

import com.intellij.codeInspection.htmlInspections.RequiredAttributesInspection;
import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.siyeh.ig.inheritance.InterfaceNeverImplementedInspection;

import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_7;

@GwtTestOptions(version = VERSION_2_7)
public class GwtUiBinderInspectionsTest extends GwtCodeInsightTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    allowTreeAccessToGwtJreEmulationLibrary();
  }

  public void testBaseMessagesInterfaceAttribute() {
    myCodeInsightFixture.configureByFile("xxx/client/MyComponentWithMessages.ui.xml");
    myCodeInsightFixture.checkHighlighting();
  }

  public void testDoNotHighlightUiBinderInterfaceAsNotImplemented() {
    myCodeInsightFixture.configureByFile("xxx/client/MyBinderComponent.java");
    myCodeInsightFixture.enableInspections(InterfaceNeverImplementedInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testDoNotHighlightUiRendererInterfaceAsNotImplemented() {
    myCodeInsightFixture.configureByFile("xxx/client/MyRendererComponent.java");
    myCodeInsightFixture.enableInspections(InterfaceNeverImplementedInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testHighlightInvalidWidgetsInTags() {
    myCodeInsightFixture.configureByFile("xxx/client/MyComponentWithInvalidTags.ui.xml");
    myCodeInsightFixture.enableInspections(UiXmlUnresolvedReferencesInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testHighlightInvalidUiBinderRoot() {
    myCodeInsightFixture.configureByFile("xxx/client/UiBinderErrorsComponent.java");
    myCodeInsightFixture.enableInspections(GwtUiBinderErrorsInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testRequiredAttributeWithoutUiFactory() {
    myCodeInsightFixture.configureByFiles("xxx/client/attributes/WithoutUiFactory.ui.xml");
    myCodeInsightFixture.enableInspections(RequiredAttributesInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testRequiredAttributeWithUiFactory() {
    myCodeInsightFixture.configureByFiles("xxx/client/attributes/WithUiFactory.ui.xml");
    myCodeInsightFixture.enableInspections(RequiredAttributesInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testRequiredAttributeWithParameterizedUiFactory() {
    myCodeInsightFixture.configureByFiles("xxx/client/attributes/WithParameterizedUiFactory.ui.xml");
    myCodeInsightFixture.enableInspections(RequiredAttributesInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testListBoxItemSubTag() {
    myCodeInsightFixture.configureByFiles("xxx/client/ListBoxItemSubTag.ui.xml");
    myCodeInsightFixture.enableInspections(UiXmlUnresolvedReferencesInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  public void testNamespaceWithDot() {
    myCodeInsightFixture.configureByFiles("xxx/client/NamespaceWithDot.ui.xml");
    myCodeInsightFixture.checkHighlighting();
  }

  public void testUiRendererUiHandler() {
    myCodeInsightFixture.configureByFiles("xxx/client/EntityCell.java", "xxx/client/EntityCell.ui.xml");
    myCodeInsightFixture.enableInspections(GwtUiHandlerErrorsInspection.class);
    myCodeInsightFixture.checkHighlighting();
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/uiBinder";
  }
}
