package com.intellij.lang.javascript.linter.jscs;

import com.intellij.lang.javascript.linter.JSLinterBaseView;
import com.intellij.lang.javascript.linter.JSLinterConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author by Irina.Chernushina on 9/22/2014.
 */
public class JscsConfigurable extends JSLinterConfigurable<JscsState> {
  @NonNls
  public static final String SETTINGS_JAVA_SCRIPT_LINTERS_JSCS = "settings.javascript.linters.jscs";

  public JscsConfigurable(Project project) {
    super(project, JscsConfiguration.class, false);
  }
  public JscsConfigurable(Project project, boolean isFullModeDialog) {
    super(project, JscsConfiguration.class, isFullModeDialog);
  }

  @NotNull
  @Override
  public String getId() {
    return SETTINGS_JAVA_SCRIPT_LINTERS_JSCS;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return JscsBundle.message("settings.javascript.linters.jscs.configurable.name");
  }

  @NotNull
  @Override
  protected JSLinterBaseView<JscsState> createView() {
    return new JscsView(getProject(), isFullModeDialog());
  }
}
