package com.intellij.dmserver.intention;

import com.intellij.codeInsight.daemon.impl.actions.AddImportAction;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.ide.hierarchy.JavaHierarchyUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.jetbrains.osgi.project.BundleManifest;
import org.jetbrains.osgi.project.BundleManifestCache;
import org.osgi.framework.Constants;
import org.osmorc.manifest.lang.psi.Clause;

/**
 * @author michael.golubev
 */
public class AddPackageIntentionAction extends IntentionAndQuickFixAction {
  private final Module myModule;
  private final SmartPsiElementPointer<PsiJavaCodeReferenceElement> myRefPointer;
  private final SmartPsiElementPointer<PsiClass> myClassPointer;
  private final LibraryOrderEntry myLibraryEntry;

  public AddPackageIntentionAction(PsiJavaCodeReferenceElement ref, PsiClass clazz, LibraryOrderEntry libraryEntry) {
    myModule = ModuleUtilCore.findModuleForPsiElement(ref);
    myRefPointer = SmartPointerManager.getInstance(ref.getProject()).createSmartPsiElementPointer(ref);
    myClassPointer = SmartPointerManager.getInstance(ref.getProject()).createSmartPsiElementPointer(clazz);
    myLibraryEntry = libraryEntry;
  }

  @Override
  @NotNull
  public String getName() {
    return DmServerBundle.message("AddPackageIntentionAction.name", getPackageName());
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return DmServerBundle.message("DMServer.QuickFix.family.name");
  }

  @Override
  public void applyFix(@NotNull Project project, PsiFile file, Editor editor) {
    new Fixer(project, editor).fix();
  }

  @Override
  public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
    return !project.isDisposed() && !myModule.isDisposed() && new Fixer(project, editor).isValid();
  }

  private String getPackageName() {
    return JavaHierarchyUtil.getPackageName(myClassPointer.getElement());
  }

  private class Fixer {
    private final Project myProject;
    private final Editor myEditor;
    private final ManifestFile myManifestFile;

    Fixer(Project project, Editor editor) {
      myProject = project;
      myEditor = editor;

      ManifestFile manifestFile = null;
      BundleManifest manifest = BundleManifestCache.getInstance().getManifest(myModule);
      if (manifest != null) {
        PsiFile source = manifest.getSource();
        if (source instanceof ManifestFile) {
          manifestFile = (ManifestFile)source;
        }
      }
      myManifestFile = manifestFile;
    }

    public boolean isValid() {
      return myManifestFile != null;
    }

    public void fix() {
      if (!isValid()) {
        return;
      }

      PsiJavaCodeReferenceElement ref = myRefPointer.getElement();
      PsiClass aClass = myClassPointer.getElement();
      if (ref == null || aClass == null) {
        return;
      }

      Header header = ManifestUtils.getInstance().findHeader(myManifestFile, Constants.IMPORT_PACKAGE);
      if (header == null) {
        myManifestFile.add(ManifestUtils.getInstance().createHeader(myProject, Constants.IMPORT_PACKAGE, getPackageName()));
      }
      else {
        StringBuilder importPackagesBuilder = new StringBuilder();

        importPackagesBuilder.append(header.getText());
        String ending = "";
        if (importPackagesBuilder.charAt(importPackagesBuilder.length() - 1) == '\n') {
          ending = "\n";
          importPackagesBuilder.replace(importPackagesBuilder.length() - 1, importPackagesBuilder.length(), "");
        }
        if (PsiTreeUtil.getChildOfType(header, Clause.class) == null) {
          if (importPackagesBuilder.charAt(importPackagesBuilder.length() - 1) != ' ') {
            importPackagesBuilder.append(" ");
          }
        }
        else {
          importPackagesBuilder.append(", ");
        }

        importPackagesBuilder.append(getPackageName());
        importPackagesBuilder.append(ending);

        header.replace(ManifestUtils.getInstance().createHeader(myProject, importPackagesBuilder));
      }

      OrderEntryUtil.addLibraryToRoots(myLibraryEntry, myModule); // TODO: replace with working add import

      if (myEditor != null) {
        new AddImportAction(myProject, ref, myEditor, aClass).execute();
      }
    }
  }
}
