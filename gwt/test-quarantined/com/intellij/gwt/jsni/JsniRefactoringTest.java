package com.intellij.gwt.jsni;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.refactoring.safeDelete.SafeDeleteHandler;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/jsni/refactoring/")
public class JsniRefactoringTest extends GwtCodeInsightTestCase {
  public void testSafeDeleteClassUsedFromInjectedCode() {
    final PsiFile file = myCodeInsightFixture.configureByFile("SafeDeleteClass.java");
    final VirtualFile virtualFile = file.getVirtualFile();
    assertNotNull(virtualFile);
    assertTrue(virtualFile.isValid());
    final PsiClass aClass = ((PsiJavaFile)file).getClasses()[0];
    SafeDeleteHandler.invoke(aClass.getProject(), new PsiElement[]{aClass}, false);
    assertFalse(virtualFile.isValid());
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "jsni/refactoring";
  }
}
