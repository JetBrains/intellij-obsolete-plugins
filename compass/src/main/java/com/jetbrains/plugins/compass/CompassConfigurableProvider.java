package com.jetbrains.plugins.compass;

import com.intellij.application.options.ModuleAwareProjectConfigurable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompassConfigurableProvider extends ConfigurableProvider {
  public static final String HELP_TOPIC = "compass.support";
  @NotNull private final Project myProject;

  public CompassConfigurableProvider(@NotNull Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Configurable createConfigurable() {
    return new ModuleAwareProjectConfigurable(myProject, "Compass", HELP_TOPIC) {
      @NotNull
      @Override
      protected UnnamedConfigurable createModuleConfigurable(Module module) {
        return CompassUtil.createCompassConfigurable(module, true);
      }
    };
  }
}