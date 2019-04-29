/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.run;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import icons.J2meIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class J2MEConfigurationType extends SimpleConfigurationType {
  J2MEConfigurationType() {
    //noinspection SpellCheckingInspection
    super("#com.intellij.j2meplugin.run.J2MEConfigurationType",
          J2MEBundle.message("run.configuration.title"),
          J2MEBundle.message("run.configuration.full.name"),
          NotNullLazyValue.createValue(() -> J2meIcons.Small_mobile));
  }

  @Override
  @NotNull
  public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new J2MERunConfiguration("", project, this);
  }

  @Override
  public boolean isApplicable(@NotNull Project project) {
    return ModuleUtil.hasModulesOfType(project, J2MEModuleType.getInstance());
  }

  @NotNull
  @Override
  public RunConfiguration createConfiguration(@Nullable String name, @NotNull RunConfiguration template) {
    J2MERunConfiguration j2MERunConfiguration = (J2MERunConfiguration)template;
    final Module[] modules = j2MERunConfiguration.getModules();
    if (j2MERunConfiguration.getModule() == null) {
      if (modules.length > 0) {
        j2MERunConfiguration.setModule(modules[0]);
      }
    }
    final Module module = j2MERunConfiguration.getModule();
    if (module != null) {
      j2MERunConfiguration.JAD_NAME = Objects.requireNonNull(MobileModuleSettings.getInstance(module)).getMobileDescriptionPath();
    }
    return super.createConfiguration(name, j2MERunConfiguration);
  }

  @NotNull
  @Override
  public String getTag() {
    return "j2me";
  }

  @Override
  public String getHelpTopic() {
    //noinspection SpellCheckingInspection
    return "reference.dialogs.rundebug.#com.intellij.j2meplugin.run.J2MEConfigurationType";
  }
}

