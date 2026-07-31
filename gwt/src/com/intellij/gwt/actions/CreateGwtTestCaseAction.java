package com.intellij.gwt.actions;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.IdeView;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CreateGwtTestCaseAction extends GwtCreateActionBase {
  private static final @NonNls String GWT_MODULE_PARAMETER = "GWT_MODULE_NAME";

  @Override
  protected PsiElement @NotNull [] doCreate(final String newName, final PsiDirectory directory, final GwtModule gwtModule) throws Exception {
    Module module = gwtModule.getModule();
    if (module != null) {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      PsiClass testCaseClass = JavaPsiFacade.getInstance(module.getProject()).findClass("junit.framework.TestCase", scope);
      if (testCaseClass == null) {
        String url = VfsUtil.getUrlForLibraryRoot(new File(JavaSdkUtil.getJunit4JarPath()));
        ModuleRootModificationUtil.addModuleLibrary(module, url);
      }
    }
    return new PsiElement[]{
        createClassFromTemplate(directory, newName, JavaFileType.INSTANCE, GwtTemplates.GWT_TEST_CASE_JAVA,
                                GWT_MODULE_PARAMETER, gwtModule.getQualifiedName())
    };
  }

  @Override
  protected boolean isAvailable(final DataContext dataContext) {
    if (!super.isAvailable(dataContext)) {
      return false;
    }

    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null || project == null) {
      return false;
    }

    PsiDirectory[] directories = view.getDirectories();
    for (PsiDirectory psiDirectory : directories) {
      VirtualFile directory = psiDirectory.getVirtualFile();
      if (ProjectRootsUtil.isInTestSource(directory, project)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("dialog.promt.enter.name.for.gwt.test.case");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("dialog.title.new.gwt.test.case");
  }

  @Override
  protected @NotNull String getActionName(final @NotNull PsiDirectory directory, final @NotNull String newName) {
    return GwtBundle.message("action.progress.creating.gwt.test.case.0", newName);
  }
}
