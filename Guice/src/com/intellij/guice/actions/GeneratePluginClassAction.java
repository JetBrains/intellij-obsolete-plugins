// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Supplier;

public abstract class GeneratePluginClassAction extends CreateElementActionBase {

  // length == 1 is important to make MyInputValidator close the dialog when
  // module selection is canceled. That's some weird interface actually...
  private static final PsiElement[] CANCELED = new PsiElement[1];

  protected GeneratePluginClassAction(@NotNull Supplier<String> dynamicText, @NotNull Supplier<String> dynamicDescription, Icon icon) {
    super(dynamicText, dynamicDescription, icon);
  }

  @Override
  protected PsiElement @NotNull [] invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory) {
    final PsiElement[] psiElements = invokeDialogImpl(project, directory);
    if (psiElements == CANCELED) {
      return PsiElement.EMPTY_ARRAY;
    }

    //   new EditorCaretMover(project).openInEditor(psiElements[0]);
    return psiElements;
  }

  protected abstract PsiElement[] invokeDialogImpl(Project project, PsiDirectory directory);

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    final Presentation presentation = e.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }
    final DataContext context = e.getDataContext();
    final IdeView view = LangDataKeys.IDE_VIEW.getData(context);
    final Project project = context.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    final Module module = e.getData(PlatformCoreDataKeys.MODULE);
    if (!AbstractNewGuiceClassAction.hasGuice(module)) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final PsiDirectory[] dirs = view.getDirectories();
    for (PsiDirectory dir : dirs) {
      if (projectFileIndex.isInSourceContent(dir.getVirtualFile()) && JavaDirectoryService.getInstance().getPackage(dir) != null) {
        presentation.setEnabledAndVisible(true);
        return;
      }
    }

    presentation.setEnabledAndVisible(false);
  }
}