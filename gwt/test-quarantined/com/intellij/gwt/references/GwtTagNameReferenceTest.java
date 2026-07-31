package com.intellij.gwt.references;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor;
import com.intellij.refactoring.move.moveClassesOrPackages.SingleSourceRootMoveDestination;
import com.intellij.refactoring.rename.RenameProcessor;

import java.util.function.BiConsumer;

import static com.intellij.openapi.util.text.StringUtil.toLowerCase;
import static com.intellij.util.containers.ContainerUtil.ar;

public class GwtTagNameReferenceTest extends GwtCodeInsightTestCase {

  public void testRenameClass() {
    doTest("MyComponent.ui.xml", "MyComponent_rename_after.ui.xml", "pkg.xxx.MyWidget", this::rename, "MyWidget1");
  }

  public void testRenameClassWithLongReference() {
    doTest("MyComponent.ui.xml", "MyComponent_rename_sub_after.ui.xml", "pkg.xxx.sub.MySubWidget", this::rename, "MySubWidget1");
  }

  public void testMoveClass() {
    doTest("MyComponent.ui.xml", "MyComponent_move_after.ui.xml", "pkg.xxx.MyWidget", this::move, "pkg.xxx.sub");
  }

  public void testMoveClassToExistingNamespace() {
    doTest("MyComponentWithImportedSub.ui.xml", "MyComponent_move_imported_after.ui.xml", "pkg.xxx.MyWidget", this::move, "pkg.xxx.sub");
  }

  public void testMoveClassToNewNamespaceWithDuplicatedName() {
    doTest("MyComponentWithImportedSub.ui.xml", "MyComponent_move_duplicated_after.ui.xml", "pkg.xxx.MyWidget", this::move, "pkg.xxx.sub.sub");
  }

  public void testMoveClassWithLongReference() {
    doTest("MyComponent.ui.xml", "MyComponent_move_sub_after.ui.xml", "pkg.xxx.sub.MySubWidget", this::move, "pkg.xxx.yyy");
  }

  public void testRenamePackage() {
    doTest("MyComponent.ui.xml", "MyComponent_ren_package_after.ui.xml", "pkg.xxx.sub", this::rename, "pkg.xxx.zzz");
  }

  public void testMovePackage() {
    doTest("MyComponent.ui.xml", "MyComponent_move_package_after.ui.xml", "pkg.xxx.sub", this::move, "pkg.yyy");
  }

  public void testMoveSubPackage() {
    doTest("MyComponentWithDeepWidget.ui.xml", "MyComponent_move_sub_package_after.ui.xml", "pkg.xxx.sub.sub", this::move, "pkg.yyy");
  }

  private void doTest(String uiXmlBefore, String uiXmlAfter, String elementName, BiConsumer<PsiElement, String> op, String newValue) {
    myCodeInsightFixture.configureByFile(uiXmlBefore);
    if (toLowerCase(elementName).equals(elementName)) {
      op.accept(findPackage(elementName), newValue);
    } else {
      op.accept(findClass(elementName), newValue);
    }
    myCodeInsightFixture.checkResultByFile(uiXmlAfter);
  }

  private void rename(PsiElement element, String newName) {
    Project project = myCodeInsightFixture.getProject();
    new RenameProcessor(project, element, newName, false, false).run();
  }

  private void move(PsiElement element, String newPackageName) {
    Project project = myCodeInsightFixture.getProject();

    PsiPackage newPackage = findPackage(newPackageName);
    PsiDirectory newPackageDirectory = assertOneElement(newPackage.getDirectories());
    MoveDestination moveDestination =
      new SingleSourceRootMoveDestination(PackageWrapper.create(newPackage), newPackageDirectory);

    new MoveClassesOrPackagesProcessor(project, ar(element), moveDestination, false, false, null).run();
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/tagNameReference";
  }
}
