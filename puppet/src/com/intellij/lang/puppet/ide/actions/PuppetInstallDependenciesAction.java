package com.intellij.lang.puppet.ide.actions;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.adapters.PuppetDependencyManagerAdapter;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.lang.puppet.project.PuppetProjectManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetInstallDependenciesAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PuppetEntity puppetEntity = getPuppetEntityFromEvent(e);
    assert puppetEntity != null;
    PuppetDependencyManagerAdapter dependencyManager = puppetEntity.getDependencyManager();
    assert dependencyManager != null;
    dependencyManager.installDependencies(puppetEntity);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    PuppetEntity puppetEntity = getPuppetEntityFromEvent(e);
    if (puppetEntity == null || puppetEntity.getDependencyManager() == null) {
      presentation.setEnabledAndVisible(false);
    }
    else {
      presentation.setEnabledAndVisible(true);
      presentation.setText(PuppetBundle.messagePointer("puppet.action.install.dependencies",
                                                       puppetEntity.getDescriptiveName(),
                                                       puppetEntity.getName()));
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  protected static @Nullable PuppetEntity getPuppetEntityFromEvent(@NotNull AnActionEvent e) {
    VirtualFile eventFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (eventFile == null || e.getProject() == null) {
      return null;
    }

    return PuppetProjectManager.getInstance(e.getProject()).findModuleOrEnvironmentForFile(eventFile);
  }
}
