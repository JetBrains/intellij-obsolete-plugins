// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure.sync;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.ui.GrailsConfigureSDKDialog;

import javax.swing.event.HyperlinkEvent;

public class GrailsSdkCheckTask extends GrailsApplicationBackgroundTask {

  public GrailsSdkCheckTask(@NotNull Project project) {
    super(project, GrailsBundle.message("progress.title.check.sdk"));
  }

  @Override
  protected void run(@NotNull GrailsApplication application, @NotNull ProgressIndicator indicator) {
    final GrailsCommandExecutor executor = GrailsCommandExecutor.getGrailsExecutor(application);
    if (executor != null) return;

    final String content = GrailsBundle.message("grails.sdk.not.found.content", application.getName());
    NotificationGroupManager.getInstance().getNotificationGroup("Grails Configure").createNotification(
      GrailsBundle.message("grails.sdk.not.found.title"), content,
      NotificationType.INFORMATION)
      .setListener(
        new NotificationListener.Adapter() {
          @Override
          protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
            new GrailsConfigureSDKDialog(application.getProject()).setGrailsApplication(application).show();
          }
        })
      .setImportant(true)
      .notify(application.getProject());
  }
}
