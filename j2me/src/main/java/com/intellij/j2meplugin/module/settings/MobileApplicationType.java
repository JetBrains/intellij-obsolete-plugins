/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings;

import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;


public abstract class MobileApplicationType {
  public static final ExtensionPointName<MobileApplicationType> MOBILE_APPLICATION_TYPE = ExtensionPointName.create("J2ME.com.intellij.applicationType");

  @NonNls
  public abstract String getName();

  @NonNls
  public abstract String getExtension();

  public abstract String getSeparator();

  @NonNls
  public abstract String getJarSizeSettingName();

  @NonNls
  public abstract String getBaseClassName();

  public boolean isUserParametersEnable() {
    return false;
  }

  public String getPresentableClassName() {
    return getBaseClassName().substring(getBaseClassName().lastIndexOf(".") + 1);
  }
  @NonNls
  public abstract String getJarUrlSettingName();

  public abstract boolean isUserField(String name);

  public abstract String createConfigurationByClass(String className);

  public abstract Class<? extends MobileModuleSettings> getClassType();

  public abstract MobileSettingsConfigurable createConfigurable(final Project project, final Module module, final MobileModuleSettings settings);

  public abstract MobileModuleSettings createTempSettings(J2MEModuleBuilder builder);
}
