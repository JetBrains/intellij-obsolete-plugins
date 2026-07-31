package com.intellij.gwt.references;

import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.jetbrains.annotations.NotNull;

public abstract class GwtCodeInsightTestCase extends GwtFixtureTestCase {
  protected JavaCodeInsightTestFixture myCodeInsightFixture;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myCodeInsightFixture = createCodeInsightFixture(getBaseDirectoryPath());
    WriteAction.runAndWait(() -> addGwtSupport());

    waitForGwtSourcePathsRefresher(myProjectFixture.getProject());
  }

  protected abstract String getBaseDirectoryPath();

  protected <T extends PsiElement> T assertResolvesTo(@TestDataFile String filePath, Class<T> resolvedClass) {
    final PsiElement resolved = myCodeInsightFixture.getReferenceAtCaretPositionWithAssertion(filePath).resolve();
    assertNotNull(resolved);
    return assertInstanceOf(resolved, resolvedClass);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      myCodeInsightFixture.tearDown();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      myCodeInsightFixture = null;
      super.tearDown();
    }
  }

  protected void assertResolvesToCssClass(final @TestDataFile String filePath, final String cssClassName) {
    assertEquals(cssClassName, assertResolvesTo(filePath, CssClass.class).getName());
  }

  protected void allowTreeAccessToGwtJreEmulationLibrary() {
    myCodeInsightFixture.allowTreeAccessForAllFiles();
  }

  @NotNull
  protected PsiClass findClass(final String className) {
    final Project project = myCodeInsightFixture.getProject();
    final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project));
    assertNotNull(className, psiClass);
    return psiClass;
  }

  @NotNull
  protected PsiPackage findPackage(String packageName) {
    PsiPackage psiPackage = JavaPsiFacade.getInstance(myCodeInsightFixture.getProject()).findPackage(packageName);
    assertNotNull(psiPackage);
    return psiPackage;
  }
}
