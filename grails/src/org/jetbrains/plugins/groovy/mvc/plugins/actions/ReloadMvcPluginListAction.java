// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginUtil;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginsMain;

public class ReloadMvcPluginListAction extends AnAction implements DumbAware {
  private final MvcPluginsMain myMvcPluginsMain;

  public ReloadMvcPluginListAction(final MvcPluginsMain mvcPluginsMain) {
    super(
      GrailsBundle.message("mvc.plugins.action.text.reload.list"),
      GrailsBundle.message("mvc.plugins.action.description.reload.list"),
      AllIcons.Actions.Refresh
    );
    myMvcPluginsMain = mvcPluginsMain;
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    Project project = myMvcPluginsMain.getProject();

    if (GrailsConsole.getInstance(project).isExecuting()) {
      Messages.showErrorDialog(project,
                               GrailsBundle.message("mvc.plugins.dialog.message.failed.to.reload.plugin.list"),
                               GrailsBundle.message("mvc.plugins.dialog.title.failed.to.execute.command")
      );
      return;
    }

    doReloadPluginList(myMvcPluginsMain);
  }

  public static void doReloadPluginList(final @NotNull MvcPluginsMain mvcPlugins) {
    assert !GrailsConsole.getInstance(mvcPlugins.getProject()).isExecuting();
    GrailsCommandExecutorUtil.executeInModal(
      mvcPlugins.getApplication(),
      new MvcCommand(MvcPluginUtil.LIST_PLUGINS_COMMAND),
      GrailsBundle.message("progress.text.updating.plugin.list"),
      mvcPlugins::reloadPlugins, true
    );
  }
}
