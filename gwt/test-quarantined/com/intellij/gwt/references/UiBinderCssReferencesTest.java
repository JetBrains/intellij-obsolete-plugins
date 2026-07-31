package com.intellij.gwt.references;

import com.intellij.psi.PsiClass;
import com.intellij.psi.css.CssFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/references/uiBinder/")
public class UiBinderCssReferencesTest extends GwtCodeInsightTestCase {
  public void testCssClassFromStyleNameAttributeResolve() {
    assertResolvesToCssClass("ppp/client/css/CssStyleNameAttribute.ui.xml", "my-button-style");
  }

  public void testCssClassFromStyleNameAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/CssStyleNameAttribute.ui.xml", "my-button-style", "my-button-style2");
  }

  public void testCssStyleAttributesCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/CssStyleAttributes.ui.xml", "addStyleNames", "addStyleDependentNames");
  }

  public void testStyleTagResolving() {
    assertResolvesTo("ppp/client/css/StyleTag.ui.xml", XmlTag.class);
  }

  public void testStyleTagCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/StyleTagCompletion.ui.xml", "style", "otherStyle");
  }

  public void testInnerCssClassResolving() {
    assertResolvesToCssClass("ppp/client/css/InnerCssClass.ui.xml", "my-inner-class");
  }

  public void testInnerCssClassCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/InnerCssClass.ui.xml", "my-inner-class", "my-inner-class2");
  }

  public void testInnerCssClassInClassAttributeResolving() {
    assertResolvesToCssClass("ppp/client/css/InnerCssClassInClassAttribute.ui.xml", "my-inner-class");
  }

  public void testInnerCssClassInClassAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/InnerCssClassInClassAttribute.ui.xml", "my-inner-class", "my-inner-class2");
  }

  public void testInnerCssClassInNamedStyleTagResolving() {
    assertResolvesToCssClass("ppp/client/css/InnerCssClassInNamedStyleTag.ui.xml", "my-inner-class");
  }

  public void testInnerCssClassInNamedStyleTagCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/InnerCssClassInNamedStyleTag.ui.xml", "my-inner-class", "my-inner-class2");
  }

  public void testExternalCssClassResolving() {
    assertResolvesToCssClass("ppp/client/css/ExternalCssClass.ui.xml", "my-external-class");
  }

  public void testExternalCssClassCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/ExternalCssClass.ui.xml", "my-external-class", "my-external-class2");
  }

  public void testSrcAttributeResolving() {
    assertResolvesTo("ppp/client/css/SrcAttribute.ui.xml", CssFile.class);
  }
  public void testSrcAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/SrcAttribute.ui.xml", "external.css", "external2.css");
  }

  public void testTypeAttributeResolving() {
    assertEquals("MyStyle", assertResolvesTo("ppp/client/css/TypeAttribute.ui.xml", PsiClass.class).getName());
  }

  public void testTypeAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/css/TypeAttribute.ui.xml", "MyStyle", "MyStyle2");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/uiBinder";
  }
}
