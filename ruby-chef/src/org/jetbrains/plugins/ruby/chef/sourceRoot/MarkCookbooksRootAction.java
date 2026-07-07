package org.jetbrains.plugins.ruby.chef.sourceRoot;

import com.intellij.ide.projectView.actions.MarkSourceRootAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MarkCookbooksRootAction extends MarkSourceRootAction {
  public MarkCookbooksRootAction() {
    super(CookbooksRootType.COOKBOOKS);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    super.actionPerformed(e);
    final Project project = e.getProject();

    if (project == null) return;
    project.getMessageBus().syncPublisher(ChefTopics.COOKBOOK).cookbookAdded();
  }
}
