/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.doja;

import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.MobileSettingsConfigurable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collections;

public class DOJAApplicationType extends MobileApplicationType {
  @NonNls
  public static final String APPLICATION_NAME = "AppName";
  @NonNls
  public static final String PACKAGE_URL = "PackageUrl";
  @NonNls
  public static final String APPLICATION_SIZE = "AppSize";
  @NonNls
  public static final String APPLICATION_CLASS = "AppClass";
  @NonNls
  public static final String LAST_MODIFIED = "LastModified";
  @NonNls
  public static final String APP_VER = "AppVersion";
  @NonNls
  public static final String CONFIGURATION_VER = "ConfigurationVer";
  @NonNls
  public static final String PROFILE_VER = "ProfileVer";
  @NonNls
  public static final String SP_SIZE = "SPsize";
  @NonNls
  public static final String APP_PARAMS = "AppParam";
  @NonNls
  public static final String USE_NETWORK = "UseNetwork";
  @NonNls
  public static final String TARGET_DEVICE = "TargetDevice";
  @NonNls
  public static final String LAUNCH_AT = "LaunchAt";
  @NonNls
  public static final String APP_TRACE = "AppTrace";
  @NonNls
  public static final String DRAW_AREA = "DrawArea";
  @NonNls
  public static final String GET_UTN = "GetUtn";
  @NonNls
  public static final String NAME = "DoJa";
  public static final String[] ADDITIONAL_SETTINGS = {
    APP_VER,
    CONFIGURATION_VER,
    PROFILE_VER,
    SP_SIZE,
    APP_PARAMS,
    USE_NETWORK,
    TARGET_DEVICE,
    LAUNCH_AT,
    APP_TRACE,
    DRAW_AREA,
    GET_UTN
  };
  private final ArrayList<String> ourFields = new ArrayList<>();


  public DOJAApplicationType() {
    ourFields.add(APPLICATION_NAME);
    ourFields.add(PACKAGE_URL);
    ourFields.add(APPLICATION_SIZE);
    ourFields.add(APPLICATION_CLASS);
    ourFields.add(LAST_MODIFIED);
    Collections.addAll(ourFields, ADDITIONAL_SETTINGS);
  }

  public static DOJAApplicationType getInstance() {
    for (MobileApplicationType applicationType : MOBILE_APPLICATION_TYPE.getExtensionList()) {
      if (applicationType instanceof DOJAApplicationType) {
        return (DOJAApplicationType)applicationType;
      }
    }
    return null;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getExtension() {
    return "jam";
  }

  @Override
  public String getSeparator() {
    return "=";
  }

  @Override
  public String getJarSizeSettingName() {
    return APPLICATION_SIZE;
  }

  @Override
  public String getBaseClassName() {
    return "com.nttdocomo.ui.IApplication";
  }

  @Override
  public String getJarUrlSettingName() {
    return PACKAGE_URL;
  }

  @Override
  public boolean isUserField(String name) {
    return !ourFields.contains(name);
  }

  @Override
  public String createConfigurationByClass(String className) {
    return APPLICATION_CLASS + "=" + className;
  }

  @Override
  public Class<? extends MobileModuleSettings> getClassType() {
    return DOJASettings.class;
  }

  @Override
  public MobileSettingsConfigurable createConfigurable(final Project project, final Module module, final MobileModuleSettings settings) {
    return new DOJASettingsConfigurable(module, settings, project);
  }

  @Override
  public MobileModuleSettings createTempSettings(final J2MEModuleBuilder builder) {
    final DOJASettings settings = new DOJASettings();
    settings.initSettings(builder);
    return settings;
  }

}
