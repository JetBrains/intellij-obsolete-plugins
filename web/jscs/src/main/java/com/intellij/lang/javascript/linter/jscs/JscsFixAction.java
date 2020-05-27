package com.intellij.lang.javascript.linter.jscs;

import com.intellij.lang.javascript.linter.JSLinterConfiguration;
import com.intellij.lang.javascript.linter.JSLinterFixAction;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Irina.Chernushina on 4/16/2015.
 */
public class JscsFixAction extends JSLinterFixAction {
  public JscsFixAction() {
    super(JscsBundle.messagePointer("settings.javascript.linters.jscs.configurable.name"), JscsBundle
      .messagePointer("jscs.action.fix.problems.description"), null);
  }

  @NotNull
  @Override
  protected JSLinterConfiguration getConfiguration(Project project) {
    return JscsConfiguration.getInstance(project);
  }

  @Override
  protected Task createTask(@NotNull Project project,
                            @NotNull Collection<? extends VirtualFile> scope,
                            @NotNull Runnable completeCallback,
                            boolean modalProgress) {
    return new JscsReformatterTask(project, scope, completeCallback).createTask(modalProgress);
  }
}
