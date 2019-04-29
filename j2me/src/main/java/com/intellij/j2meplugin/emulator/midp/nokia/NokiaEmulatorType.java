/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp.nokia;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.midp.MIDPEmulatorType;
import com.intellij.j2meplugin.emulator.ui.MobileApiSettingsEditor;
import com.intellij.j2meplugin.emulator.ui.MobileDefaultApiEditor;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.j2meplugin.run.states.midp.nokia.NokiaRunnableState;
import com.intellij.j2meplugin.run.states.midp.uei.UEIRunnableState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Properties;

public class NokiaEmulatorType extends MIDPEmulatorType {
  private static final String NAME = J2MEBundle.message("emulator.nokia.edition");
  @NonNls private static final String KVEM_CLASS_PATH_PROPERTY = "kvem.class.path";
  @NonNls private static final String PREVERIFIER_BINARY_PROPERTY = "preverifier.binary";
  @NonNls private static final String EMULATOR_BINARY_PROPERTY = "emulator.binary";
  @NonNls private static final String API_CLASS_PATH_PROPERTY = "api.class.path";
  @NonNls private static final String MICROEDITION_PROFILES_PROPERTY = "microedition.profiles";
  @NonNls private static final String MICROEDITION_CONFIGURATION_PROPERTY = "microedition.configuration";
  @NonNls private static final String DEVICE_MODEL_PROPERTY = "device.model";
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin.emulator.midp.nokia.NokiaEmulatorType");

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  @Nullable
  @NonNls
  public String getRelativePathToEmulator() {
    return null;
  }

  @Override
  @Nullable
  @NonNls
  public String getPreverifyPath() {
    return null;
  }

  @Override
  @Nullable
  @NonNls
  public String getDeviceOption() {
    return null;
  }

  @Override
  @Nullable
  public String getPrefPath(String home) {
    final String preferences = ConfigurationUtil.getPreferences(home);
    if (preferences != null) {
      return toSystemDependentPath(home, preferences);
    }
    else {
      return null;
    }
  }

  public static String getKvemPath(String home) {
    final Properties properties = ConfigurationUtil.getProperties(home);
    LOG.assertTrue(properties != null);
    final String kvemProps = properties.getProperty(KVEM_CLASS_PATH_PROPERTY);
    LOG.assertTrue(kvemProps != null);
    String[] api = kvemProps.split(";");
    String result = "";
    for (int i = 0; api != null && i < api.length; i++) {
      result += home + File.separator + api[i] + File.pathSeparator;
    }
    return FileUtil.toSystemIndependentName(result);
  }

  @Override
  @Nullable
  public String getUtilPath(String home) {
    return null;
  }

  @Override
  public String getPreverifyPath(String home) {
    final Properties properties = ConfigurationUtil.getProperties(home);
    LOG.assertTrue(properties != null);
    return toSystemDependentPath(home, ConfigurationUtil.getProperties(home).getProperty(PREVERIFIER_BINARY_PROPERTY));
  }

  @Override
  public String getPathToEmulator(String home) {
    final Properties properties = ConfigurationUtil.getProperties(home);
    LOG.assertTrue(properties != null);
    return toSystemDependentPath(home, ConfigurationUtil.getProperties(home).getProperty(EMULATOR_BINARY_PROPERTY));
  }

  @Override
  public String[] getApi(String homePath) {
    final Properties properties = ConfigurationUtil.getProperties(homePath);
    LOG.assertTrue(properties != null);
    String[] api = ConfigurationUtil.getProperties(homePath).getProperty(API_CLASS_PATH_PROPERTY).split(";");
    String result = "";
    for (int i = 0; api != null && i < api.length; i++) {
      result += homePath + File.separator + api[i] + File.pathSeparator;
    }
    return result.replace(File.separatorChar, '/').split(File.pathSeparator);
  }

  @Override
  public String getDefaultProfile(@NotNull String homePath) {
    final Properties properties = ConfigurationUtil.getProperties(homePath);
    LOG.assertTrue(properties != null);
    return ConfigurationUtil.getProperties(homePath).getProperty(MICROEDITION_PROFILES_PROPERTY);
  }

  @Override
  public String getDefaultConfiguration(@NotNull String homePath) {
    final Properties properties = ConfigurationUtil.getProperties(homePath);
    LOG.assertTrue(properties != null);
    return ConfigurationUtil.getProperties(homePath).getProperty(MICROEDITION_CONFIGURATION_PROPERTY);
  }

  @Override
  public MobileApiSettingsEditor getApiEditor(final String homePath, Sdk sdk, SdkModificator sdkModificator) {
    return new MobileDefaultApiEditor();
  }

  @Override
  public boolean isValidHomeDirectory(String homePath) {
    final Properties emulatorProperties = ConfigurationUtil.getProperties(homePath);
    if (emulatorProperties == null || emulatorProperties.isEmpty()) return false;
    if (emulatorProperties.getProperty(DEVICE_MODEL_PROPERTY) == null) return false;
    if (emulatorProperties.getProperty(MICROEDITION_CONFIGURATION_PROPERTY) == null) return false;
    if (emulatorProperties.getProperty(MICROEDITION_PROFILES_PROPERTY) == null) return false;
    if (emulatorProperties.getProperty(API_CLASS_PATH_PROPERTY) == null) return false;
    if (emulatorProperties.getProperty(EMULATOR_BINARY_PROPERTY) == null) return false;
    if (emulatorProperties.getProperty(PREVERIFIER_BINARY_PROPERTY) == null) return false;
    if (emulatorProperties.getProperty(KVEM_CLASS_PATH_PROPERTY) == null) return false;
    return true;
  }

  @Override
  @Nullable
  public String[] getAvailableSkins(final String homePath) {
    return null;
  }

  @Override
  public J2MERunnableState getJ2MERunnableState(Executor executor,
                                                RunnerSettings runnerSettings,
                                                J2MERunConfiguration configuration,
                                                Project project,
                                                Sdk projectJdk) {
    final String uei = getPathToEmulator(projectJdk.getHomePath());
    if (uei != null && uei.length() != 0) {
      return new UEIRunnableState(
        runnerSettings,
        configuration,
                                  project,
                                  projectJdk);
    }
    else {
      return new NokiaRunnableState(
        runnerSettings,
        configuration,
                                    project,
                                    projectJdk);
    }
  }
}
