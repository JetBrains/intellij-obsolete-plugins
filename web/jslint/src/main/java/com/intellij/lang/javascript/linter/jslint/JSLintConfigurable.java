package com.intellij.lang.javascript.linter.jslint;

import com.intellij.lang.javascript.linter.JSLinterBaseView;
import com.intellij.lang.javascript.linter.JSLinterConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Simonchik
 */
public class JSLintConfigurable extends JSLinterConfigurable<JSLintState> {

  public JSLintConfigurable(@NotNull Project project) {
    super(project, JSLintConfiguration.class, false);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return JSLintBundle.message("settings.javascript.linters.jslint.configurable.name");
  }

  @NotNull
  @Override
  public String getId() {
    return "Settings.JavaScript.Linters.JSLint";
  }

  @NotNull
  @Override
  protected JSLinterBaseView<JSLintState> createView() {
    return new JSLintView();
  }
}
