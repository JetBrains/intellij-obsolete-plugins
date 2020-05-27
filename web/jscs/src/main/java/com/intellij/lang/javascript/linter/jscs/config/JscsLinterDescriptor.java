package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil;
import com.intellij.lang.javascript.linter.JSLinterConfiguration;
import com.intellij.lang.javascript.linter.JSLinterDescriptor;
import com.intellij.lang.javascript.linter.jscs.JscsConfiguration;
import com.intellij.lang.javascript.linter.jscs.JscsBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Irina.Chernushina on 11/24/2016.
 */
public class JscsLinterDescriptor extends JSLinterDescriptor {

  @NotNull
  @Override
  public String getDisplayName() {
    return JscsBundle.message("settings.javascript.linters.jscs.configurable.name");
  }

  @NotNull
  @Override
  public String packageName() {
    return "jscs";
  }

  @Nullable
  @Override
  public String packageJsonSectionName() {
    return "jscsConfig";
  }

  @Override
  public boolean hasConfigFiles(@NotNull Project project) {
    return JSLinterConfigFileUtil.projectHasConfigFiles(project, JscsConfigFileType.INSTANCE);
  }

  @NotNull
  @Override
  public Class<? extends JSLinterConfiguration> getConfigurationClass() {
    return JscsConfiguration.class;
  }
}
