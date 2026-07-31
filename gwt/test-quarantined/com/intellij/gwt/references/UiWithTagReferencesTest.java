package com.intellij.gwt.references;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/references/uiBinder/")
public class UiWithTagReferencesTest extends GwtCodeInsightTestCase {
  public void testTypeAttributeResolving() {
    assertEquals("MyRes", assertResolvesTo("ppp/client/uiWith/TypeAttribute.ui.xml", PsiClass.class).getName());
  }

  public void testTypeAttributeCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiWith/TypeAttribute.ui.xml", "MyRes", "MyRes2");
  }

  public void testClientBundleMethodResolving() {
    assertResolvesTo("ppp/client/uiWith/ClientBundleResource.ui.xml", PsiMethod.class);
  }

  public void testClientBundleMethodCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiWith/ClientBundleResource.ui.xml", "logo", "logo2");
  }

  public void testQualifiedReferenceWithSuffixResolving() {
    assertResolvesTo("ppp/client/uiWith/QualifiedReferenceWithSuffix.ui.xml", PsiMethod.class);
  }

  public void testQualifiedReferenceWithSuffixCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiWith/QualifiedReferenceWithSuffix.ui.xml", "getMyAttribute", "getMyAttribute2");
  }

  public void testCssClassMethodResolving() {
    assertResolvesTo("ppp/client/uiWith/CssClassResource.ui.xml", PsiMethod.class);
  }

  public void testCssClassMethodCompletion() {
    myCodeInsightFixture.testCompletionVariants("ppp/client/uiWith/CssClassResource.ui.xml", "mainBlock", "mainBlock2");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/uiBinder";
  }
}
