// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.ui.GrailsConfigureSDKDialog;
import org.jetbrains.plugins.grails.util.version.Version;

public class GrailsConfigureSDKAction extends DumbAwareAction {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (e.getProject() != null) {
      for (GrailsApplication application : GrailsApplicationManager.getInstance(e.getProject()).getApplications()) {
        if (application.getGrailsVersion().isLessThan(Version.GRAILS_3_0) || GrailsCommandExecutor.getGrailsExecutor(application) == null) {
          e.getPresentation().setEnabledAndVisible(true);
          return;
        }
      }
    }
    e.getPresentation().setEnabledAndVisible(false);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) return;

    final GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    new GrailsConfigureSDKDialog(project).setGrailsApplication(application).show();
  }
}
