package com.intellij.gwt.references;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.refactoring.rename.RenameProcessor;

public class GwtRenamePackageTest extends GwtCodeInsightTestCase {

  public void testUiXmlNamespace() {
    myCodeInsightFixture.configureByFile("MyComponent.ui.xml");
    String oldPackageName = "pkg";
    String newPackageName = "pkg2";

    Project project = myCodeInsightFixture.getProject();

    PsiPackage pkg = JavaPsiFacade.getInstance(project).findPackage(oldPackageName);
    assertNotNull("Package " + oldPackageName + " not found", pkg);

    new RenameProcessor(project, pkg, newPackageName, true, true).run();

    myCodeInsightFixture.checkResultByFile("MyComponent_after.ui.xml");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/renamePackage";
  }
}
