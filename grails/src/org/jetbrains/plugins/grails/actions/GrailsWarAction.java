// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public class GrailsWarAction extends AnAction {
  private static final @NonNls String WAR_TARGET = "war";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    GrailsCommandExecutor executor = GrailsCommandExecutor.getGrailsExecutor(application);
    if (executor == null) return;

    try {
      executor.execute(application, new MvcCommand(WAR_TARGET), null, true);
    }
    catch (ExecutionException ex) {
      GrailsConsole.NOTIFICATION_GROUP
        .createNotification(GrailsBundle.message("notification.title.failed.to.execute.grails.war"), ex.getMessage(), NotificationType.WARNING)
        .notify(e.getProject());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    e.getPresentation().setVisible(GrailsCommandExecutor.getGrailsExecutor(application) != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
