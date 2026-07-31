package com.intellij.gwt.references;

import com.intellij.gwt.clientBundle.css.language.psi.GwtCssDef;
import com.intellij.gwt.clientBundle.jam.ClientBundleMethodJamElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.css.CssFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/references/clientBundle/")
public class ClientBundleReferencesTest extends GwtCodeInsightTestCase {
  public void testCssClassInClassNameResolve() {
    assertResolvesToCssClass("client/MyCssResource.java", "my-class");
  }

  public void testCssClassInClassNameCompletion() {
    myCodeInsightFixture.testCompletionVariants("client/MyCssResource.java", "my-class", "my-class-2");
  }

  public void testDefInCssResolving() {
    assertResolvesTo("client/def.css", GwtCssDef.class);
  }

  public void testDefInCssCompletion() {
    myCodeInsightFixture.testCompletionVariants("client/def.css", "small", "small2");
  }

  public void testRenameCssClass() {
    myCodeInsightFixture.configureByFile("client/renameCssClass/methodName.css");
    myCodeInsightFixture.renameElement(myCodeInsightFixture.getElementAtCaret(), "newName", true, true);
    myCodeInsightFixture.checkResultByFile("client/renameCssClass/MethodName.java", "client/renameCssClass/MethodName_after.java", true);
  }

  public void testRenameMethodForCssClass() {
    myCodeInsightFixture.configureByFile("client/renameMethod/MethodName.java");
    myCodeInsightFixture.renameElement(myCodeInsightFixture.getElementAtCaret(), "newName", true, true);
    myCodeInsightFixture.checkResultByFile("client/renameMethod/methodName.css", "client/renameMethod/methodName_after.css", true);
  }

  //IDEA-61460
  public void testFindFileByNameInConstant() {
    myCodeInsightFixture.configureByFile("client/FileNameInConstantBundle.java");
    final PsiMethod method = PsiTreeUtil.getParentOfType(myCodeInsightFixture.getElementAtCaret(), PsiMethod.class, false);
    assertNotNull(method);
    final ClientBundleMethodJamElement element = ClientBundleMethodJamElement.getElement(method);
    assertNotNull(element);
    final CssFile cssFile = assertInstanceOf(assertOneElement(element.getSourceFiles(false)), CssFile.class);
    assertEquals("app.css", cssFile.getName());
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/clientBundle";
  }
}
