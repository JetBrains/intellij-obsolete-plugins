package com.intellij.gwt.make.report;

import com.intellij.CommonBundle;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GenerateCompileReportAction extends AnAction {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null &&
                                             !ProjectFacetManager.getInstance(project).getFacets(GwtFacetType.ID).isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) return;

    List<GwtModule> modules = new ArrayList<>();
    for (Module module : ProjectFacetManager.getInstance(project).getModulesWithFacet(GwtFacetType.ID)) {
      final List<GwtModule> gwtModules = GwtModulesManager.getInstance(project).getGwtModules(module, false);
      modules.addAll(gwtModules);
    }

    if (modules.isEmpty()) {
      Messages.showErrorDialog(GwtBundle.message("dialog.title.no.gwt.modules.found.in.the.project"), CommonBundle.getErrorTitle());
      return;
    }
    new GenerateCompilerReportDialog(project, modules).show();
  }
}
