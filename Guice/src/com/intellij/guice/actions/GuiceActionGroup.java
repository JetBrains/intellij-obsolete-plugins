// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;

public final class GuiceActionGroup extends DefaultActionGroup {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final Presentation presentation = e.getPresentation();
    if (project != null && view != null) {
      final Module module = e.getData(PlatformCoreDataKeys.MODULE);
      if (!AbstractNewGuiceClassAction.hasGuice(module)) {
        presentation.setEnabledAndVisible(false);
        return;
      }

      final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      final PsiDirectory[] dirs = view.getDirectories();
      for (PsiDirectory dir : dirs) {
        if (projectFileIndex.isInSourceContent(dir.getVirtualFile()) && JavaDirectoryService.getInstance().getPackage(dir) != null) {
          presentation.setVisible(true);
          return;
        }
      }
    }
    presentation.setVisible(false);
  }
}