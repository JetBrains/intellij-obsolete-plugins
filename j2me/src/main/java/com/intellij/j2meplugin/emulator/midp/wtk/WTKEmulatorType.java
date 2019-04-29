/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp.wtk;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.midp.MIDPEmulatorType;
import com.intellij.j2meplugin.emulator.midp.uei.UnifiedEmulatorType;
import com.intellij.j2meplugin.emulator.ui.MobileApiSettingsEditor;
import com.intellij.j2meplugin.emulator.ui.MobileDefaultApiEditor;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.j2meplugin.run.states.midp.uei.UEIRunnableState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Properties;


public class WTKEmulatorType extends MIDPEmulatorType {
  @NonNls private static final String PROFILES = "profiles";
  @NonNls private static final String CONFIGURATIONS = "configurations";
  private static final String EMULATOR_TYPE_NAME = J2MEBundle.message("emulator.wtk.fullname");
  //@NonNls public static final String KVEM_PATH = "wtklib/kenv.zip;/wtklib/ktools.zip";

  @Override
  @NonNls
  public String getName() {
    return EMULATOR_TYPE_NAME;
  }

  @Override
  @Nullable
  @NonNls
  public String getUtilPath(final String home) {
    return toSystemDependentPath(home, "bin/utils");
  }

  @Override
  @Nullable
  @NonNls
  public String getPrefPath(final String home) {
    return toSystemDependentPath(home, "bin/prefs");
  }

  @Override
  public boolean isValidHomeDirectory(String homePath) {
    return ConfigurationUtil.isValidWTKHome(homePath);
  }

  @Override
  public String[] getAvailableSkins(final String homePath) {
    final String exePath = getPathToEmulator(homePath);
    if (exePath != null) {
      final String[] devices = UnifiedEmulatorType.fillEmulatorDevices(exePath);
      if (devices != null) return devices;
    }
    return ConfigurationUtil.getWTKDevices(homePath);
  }

  @Override
  public String[] getOTACommands() {
    return new String[]{UnifiedEmulatorType.INSTALL, UnifiedEmulatorType.FORCE, UnifiedEmulatorType.RUN, UnifiedEmulatorType.REMOVE, UnifiedEmulatorType.TRANSIENT, UnifiedEmulatorType.STORAGE_NAMES};
  }

  @Override
  public String[] getApi(String homePath) {
    return ConfigurationUtil.getDefaultApiPath(homePath);
  }

  @Override
  public MobileApiSettingsEditor getApiEditor(String homePath, Sdk sdk, SdkModificator sdkModificator) {
    final Properties apiSettings = ConfigurationUtil.getApiSettings(homePath);
    if (apiSettings == null || apiSettings.isEmpty()) {
      return new MobileDefaultApiEditor();
    }
    return new WTKApiEditor(this, sdk, sdkModificator);
  }

  @Override
  public J2MERunnableState getJ2MERunnableState(Executor executor,
                                                RunnerSettings runnerSettings,
                                                J2MERunConfiguration configuration,
                                                Project project,
                                                Sdk projectJdk) {
    return new UEIRunnableState(runnerSettings, configuration, project, projectJdk);
  }

  @Override
  public String[] getAvailableProfiles(@NotNull String homePath) {
    return getExistSettings(homePath, PROFILES);
  }

  @Override
  public String[] getAvailableConfigurations(@NotNull String homePath) {
    return getExistSettings(homePath, CONFIGURATIONS);
  }

  //profiles, configurations
  @SuppressWarnings({"HardCodedStringLiteral"})
  private static String[] getExistSettings(String homePath, String settingName) {
    Properties properties = ConfigurationUtil.getApiSettings(homePath);
    if (properties == null || properties.isEmpty()) {
      return null;
    }
    String profiles = properties.getProperty(settingName);
    if (profiles == null) {
      return null;
    }
    ArrayList<String> result = new ArrayList<>();
    final String[] values = profiles.split("[, \n]");
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      final String stringInJad = properties.getProperty(value + ".jadValue");
      if (stringInJad != null && stringInJad.length() != 0){
        result.add(stringInJad.trim());
      }
    }
    return ArrayUtil.toStringArray(result);
  }


  @Override
  public String getDefaultProfile(@NotNull String homePath) {
    return ConfigurationUtil.getProfileVersion(homePath);
  }

  @Override
  public String getDefaultConfiguration(@NotNull String homePath) {
    return ConfigurationUtil.getConfigurationVersion(homePath);
  }

  public static WTKEmulatorType getInstance() {
    return ApplicationManager.getApplication().getComponent(WTKEmulatorType.class);
  }
}
