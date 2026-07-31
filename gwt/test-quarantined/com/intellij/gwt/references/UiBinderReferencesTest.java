package com.intellij.gwt.references;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/references/uiBinder/")
public class UiBinderReferencesTest extends GwtCodeInsightTestCase {
  public void testUiFieldInHtmlTag() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/UiFieldAttribute.ui.xml", "field");
  }

  public void testComponentAttribute() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/ComponentAttribute.ui.xml", "unit");
  }

  public void testTagAttributeFromSchema() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/TagAttributeFromSchema.ui.xml", "size");
  }

  public void testTagFromSchema() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/TagFromSchema.ui.xml", "center", "east", "north", "south", "west");
  }

  public void testComponentInnerTag() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/StandardComponentInnerTag.ui.xml", "InlineLabel");
  }

  public void testCustomWidgetTag() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/CustomWidgetTag.ui.xml", "MyWidget");
  }

  public void testCustomWidgetTagCompleteAll() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/CustomWidgetTagCompleteAll.ui.xml", "MyWidget", "parameters.MyParametrizedWidget");
  }

  public void testHtmlTag() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/HtmlTag.ui.xml", "strong");
  }

  public void testComponentTag() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/ComponentTag.ui.xml", "DockLayoutPanel");
  }

  public void testComponentInitializerAttribute() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/CustomComponentInitializer.ui.xml", "initializer");
  }

  public void testComponentPropertyAttribute() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/CustomComponentProperty.ui.xml", "myProperty");
  }

  public void testComponentUiFieldAttribute() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/CustomComponentUiField.ui.xml", "field");
  }

  public void testUiFieldAttributeValue() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/MyComponent.ui.xml", "nameSpan");
  }

  public void testUiHandlerAnnotationValue() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/MyComponent.java", "myButton");
  }

  public void testUiFieldAttributeValueWithUiTemplate() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiTemplate/MyComponent.ui.xml", "nameSpan");
  }

  public void testUiHandlerAnnotationValueWithUiTemplate() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiTemplate/MyComponentImpl.java", "myButton");
  }

  public void testUiTemplatePathResolving() {
    final XmlFile file = assertResolvesTo("ppp/client/uiTemplate/TemplateAnnotation.java", XmlFile.class);
    assertEquals("Template.ui.xml", file.getName());
  }

  public void testUiTemplatePathCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiTemplate/TemplateAnnotation.java", "Template.ui.xml", "Template2.ui.xml");
  }

  public void testUiFieldInXmlResolving() {
    final XmlTag tag = assertResolvesTo("ppp/client/FieldInXml.java", XmlTag.class);
    assertEquals("span", tag.getAttributeValue("ui:field"));
  }

  public void testUiFieldInXmlCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/FieldInXml.java", "span", "span2");
  }

  public void testUiFieldInSuperClassFromAttributeResolving() {
    assertEquals("nameSpanField", assertResolvesTo("ppp/client/inheritance/MyComponent.ui.xml", PsiField.class).getName());
  }

  public void testUiFieldInSuperClassFromAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/inheritance/MyComponent.ui.xml", "nameSpanField", "nameSpanField2");
  }

  public void testUiFieldInSuperClassFromHandlerResolving() {
    assertEquals("myButtonField", assertResolvesTo("ppp/client/inheritance/MyComponent.java", PsiField.class).getName());
  }

  public void testUiFieldInSuperClassFromHandlerCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/inheritance/MyComponent.java", "myButtonField", "myButtonField2");
  }

  public void testClassNameCompletionForCustomComponent() {
    doTestClassNameCompletion("ppp/client/xml/ComponentWithNamespace.ui.xml", "\n");
  }

  public void testClassNameCompletionForStandardComponent() {
    doTestClassNameCompletion("ppp/client/xml/StandardComponentWithNamespace.ui.xml", "");
  }

  public void testClassNameCompletionForComponentFromExistingNamespace() {
    doTestClassNameCompletion("ppp/client/xml/ComponentFromExistingNamespace.ui.xml", "\n");
  }

  public void testClassNameReplaceCompletionForComponentFromExistingNamespace() {
    doTestClassNameCompletion("ppp/client/xml/ComponentWithNamespaceReplace.ui.xml", "\t");
  }

  public void testClassNameReplaceCompletionForNestedComponent() {
    doTestClassNameCompletion("ppp/client/xml/ComponentWithNestedPackagesReplace.ui.xml", "\t");
  }

  public void testAttributeNameReplaceCompletion() {
    doTestAttributeNameCompletion("ppp/client/xml/AttributeNameReplaceCompletion.ui.xml", "\t");
  }

  public void testEnumAttributeValueCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/EnumAttributeValue.ui.xml", "CENTER", "LEFT", "RIGHT");
  }

  public void testCustomEnumAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/CustomEnumAttribute.ui.xml", "direction");
  }

  public void testIntegerAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/xml/IntegerAttribute.ui.xml", "intProperty");
  }

  public void testSingleUiImport() {
    assertEquals("name", assertResolvesTo("ppp/client/uiImport/UiImportTag.ui.xml", PsiMethod.class).getName());
  }

  public void testWildcardUiImport() {
    assertEquals("name", assertResolvesTo("ppp/client/uiImport/UiWildcardImportTag.ui.xml", PsiMethod.class).getName());
  }

  public void testFieldReferenceInXml() {
    assertEquals("getAltText", assertResolvesTo("ppp/client/uiField/FieldReferenceInXml.ui.xml", PsiMethod.class).getName());
  }

  private void doTestClassNameCompletion(final String file, String type) {
    doTestNameCompletion(file, type, 2);
  }

  private void doTestAttributeNameCompletion(final String file, String type) {
    doTestNameCompletion(file, type, 1);
  }

  private void doTestNameCompletion(final String file, String type, int invocationCount) {
    myCodeInsightFixture.configureByFile(file);
    myCodeInsightFixture.complete(CompletionType.BASIC, invocationCount);
    myCodeInsightFixture.type(type);
    final int dot = file.indexOf('.');
    String expectedFile = file.substring(0, dot) + "_after" + file.substring(dot);
    myCodeInsightFixture.checkResultByFile(expectedFile);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/uiBinder";
  }
}
