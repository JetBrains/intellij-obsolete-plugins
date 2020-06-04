package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.javascript.linter.JSLinterDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Irina.Chernushina on 11/24/2016.
 */
public class GjsLinterDescriptor extends JSLinterDescriptor {

  @NotNull
  @Override
  public String getDisplayName() {
    return GjsLintBundle.message("settings.javascript.linters.gjslint.configurable.name");
  }

  @NotNull
  @Override
  public Class<GjsLintConfiguration> getConfigurationClass() {
    return GjsLintConfiguration.class;
  }
}
