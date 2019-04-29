/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.i18n;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.lang.properties.psi.ResourceBundleManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobileResourceBundleManager extends ResourceBundleManager{
  private static final Logger LOG = Logger.getInstance(MobileResourceBundleManager.class);

  public MobileResourceBundleManager(final Project project) {
    super(project);
  }

  @Override
  @Nullable
  public PsiClass getResourceBundle(){
    return ResourceBeansContainer.getInstance(myProject).getResourceBundle();
  }


  @Override
  @NonNls
  public String getTemplateName() {
    return "Mobile I18N.java";
  }

  @Override
  @NonNls
  public String getConcatenationTemplateName() {
    return "Mobile I18N Concatenation.java";
  }

  @Override
  public boolean isActive(PsiFile context) throws ResourceBundleNotFoundException {
    final Module module = ModuleUtil.findModuleForPsiElement(context);
    if (module != null && ModuleType.get(module) == J2MEModuleType.getInstance()) {
      if (getResourceBundle() != null) return true;
      throw new ResourceBundleNotFoundException(J2MEBundle.message("resource.bundle.not.found.dialog.title"), new SetupResourceBundleFix());
    }
    return false;
  }

  @Override
  public boolean canShowJavaCodeInfo() {
    return false;
  }



  private class SetupResourceBundleFix implements IntentionAction {
    @Override
    @NotNull
    public String getText() {
      return "";
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      final PsiDirectory containingDirectory = file.getContainingDirectory();
      final String createItem = J2MEBundle.message("resource.bundle.not.found.create");
      final String chooseItem = J2MEBundle.message("resource.bundle.not.found.choose");
      final int item = Messages.showChooseDialog(project, J2MEBundle.message("resource.bundle.not.found.dialog.message"), J2MEBundle.message("resource.bundle.not.found.dialog.title"),
                                                 Messages.getWarningIcon(), new String[]{createItem, chooseItem},
                                                 createItem);
      if (item == 0) {
        final VirtualFile virtualFile = file.getVirtualFile();
        LOG.assertTrue(virtualFile != null);
        final Module module = ModuleUtil.findModuleForFile(virtualFile, project);
        final PackageChooserDialog chooser =
          new PackageChooserDialog(J2MEBundle.message("resource.bundle.destination.chooser.title"), myProject);
        if (file instanceof PsiJavaFile) {
          chooser.selectPackage(((PsiJavaFile)file).getPackageName());
        }
        if (chooser.showAndGet()) {
          final PsiPackage aPackage = chooser.getSelectedPackage();
          if (aPackage != null) {
            final PsiDirectory[] directories = aPackage.getDirectories(GlobalSearchScope.moduleScope(module));
            if (directories.length > 0) {
              new CreateMobileResourcePackAction().invokeDialog(project, directories[0]);
            }
          }
        }
      } else if (item == 1){
        final TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project).createProjectScopeChooser(chooseItem);
        chooser.selectDirectory(containingDirectory);
        chooser.showDialog();
        final PsiClass selectedClass = chooser.getSelected();
        if (selectedClass != null) {
          ResourceBeansContainer.getInstance(project).registerResourceBundle(selectedClass);
        }
      }
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }
  }
}