// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.ui.GrailsRunCommandDialog;

import java.util.Collection;

public final class GrailsRunCommandAction extends AnAction {

  private static final String LAST_SELECTED_APP_KEY = "grails.last.selected.app.url";

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    boolean canRun = project != null && GrailsApplicationManager.getInstance(project).hasApplications();
    e.getPresentation().setEnabledAndVisible(canRun);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) return;
    final GrailsApplication application = getApplicationForSelection(e.getDataContext(), project);
    final GrailsRunCommandDialog dialog = new GrailsRunCommandDialog(project).setSelectedApplication(application);
    if (dialog.showAndGet()) {
      GrailsApplication selectedApplication = dialog.getSelectedApplication();
      PropertiesComponent.getInstance(project).setValue(LAST_SELECTED_APP_KEY, selectedApplication.getRoot().getUrl());
      GrailsCommandExecutorUtil.execute(selectedApplication, dialog.getCommand(), null, false);
    }
  }

  private static @Nullable GrailsApplication getApplicationForSelection(@NotNull DataContext context, @NotNull Project project) {
    GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(context);
    if (application != null) return application;

    GrailsApplicationManager applicationManager = GrailsApplicationManager.getInstance(project);

    Collection<GrailsApplication> applications = applicationManager.getApplications();
    if (applications.size() == 1) {
      return ContainerUtil.getFirstItem(applications);
    }

    String url = PropertiesComponent.getInstance(project).getValue(LAST_SELECTED_APP_KEY);
    if (url != null) {
      VirtualFile root = VirtualFileManager.getInstance().findFileByUrl(url);
      return applicationManager.getApplicationByRoot(root);
    }

    return null;
  }
}
