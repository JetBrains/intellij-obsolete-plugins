package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.javascript.linter.JSLinterBaseView;
import com.intellij.lang.javascript.linter.JSLinterConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Simonchik
 */
public class GjsLintConfigurable extends JSLinterConfigurable<GjsLintState> {

  public GjsLintConfigurable(@NotNull Project project) {
    this(project, false);
  }

  public GjsLintConfigurable(@NotNull Project project, boolean fullModeDialog) {
    super(project, GjsLintConfiguration.class, fullModeDialog);
  }

  @NotNull
  @Override
  public String getId() {
    return "Settings.JavaScript.Linters.ClosureLinter";
  }

  @Nls
  @Override
  public String getDisplayName() {
    return GjsLintBundle.message("settings.javascript.linters.gjslint.configurable.name");
  }

  @NotNull
  @Override
  protected JSLinterBaseView<GjsLintState> createView() {
    return new GjsLintView(getProject(), isFullModeDialog());
  }
}
