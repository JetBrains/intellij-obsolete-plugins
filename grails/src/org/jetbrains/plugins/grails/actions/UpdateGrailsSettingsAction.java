// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;

public class UpdateGrailsSettingsAction extends DumbAwareAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    final GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    e.getPresentation().setEnabledAndVisible(application instanceof OldGrailsApplication);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    if (application instanceof OldGrailsApplication) {
      GrailsFramework.forceSynchronizationSetting(((OldGrailsApplication)application).getModule());
    }
  }
}
