/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.doja;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.j2meplugin.module.settings.doja.DOJAApplicationType;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.j2meplugin.run.states.doja.DOJARunnableState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class DOJAEmulatorType extends EmulatorType {
  @NonNls private static final String ABOUT_1_PROPERTY = "ABOUT_1";
  @NonNls private static final String MAIN_WINDOW_TITLE = "MAIN_WINDOW_TITLE";
  @NonNls private static final String LIB = "lib";

  @Override
  public String getName() {
    return "DoJa";
  }

  @Override
  public String getApplicationType() {
    return DOJAApplicationType.NAME;
  }

  @Override
  public String getDescriptorOption() {
    return "-i";
  }

  @Override
  public String getDeviceOption() {
    return "-s";
  }

  @Override
  public String getRelativePathToEmulator() {
    return "bin/doja_g";
  }

  @Override
  public String suggestName(String homePath) {
    return getProperties(homePath).getProperty(ABOUT_1_PROPERTY);
  }

  private static Properties getProperties(String homePath) {
    File i18properties = new File(homePath + File.separator + LIB + File.separator + "i18n" + File.separator + "I18N.properties");
    Properties prop = new Properties();
    try {
      prop.load(new BufferedInputStream(new FileInputStream(i18properties)));
    }
    catch (IOException ignored) {
    }
    return prop;
  }

  @Override
  public boolean isValidHomeDirectory(String homePath) {
    @NonNls String property = getProperties(homePath).getProperty(MAIN_WINDOW_TITLE);
    if (property == null) return false;
    return property.equals("iappliTool");
  }

  @Override
  public String[] getAvailableSkins(final String homePath) {
    @NonNls final String skin = "skin";
    File skins = new File(new File(homePath, LIB), skin);
    if (!skins.exists() || !skins.isDirectory()){
      return new String[]{"device1", "device2", "device3"};
    }
    final String[] strings = skins.list();
    ArrayList<String> devices = new ArrayList<>();
    for (String device : strings) {
      if (new File(skins, device).isDirectory()) {
        devices.add(device);
      }
    }
    return ArrayUtil.toStringArray(devices);
  }

  @Override
  public J2MERunnableState getJ2MERunnableState(Executor executor,
                                                RunnerSettings runnerSettings,
                                                J2MERunConfiguration configuration,
                                                Project project,
                                                Sdk projectJdk) {
    return new DOJARunnableState(
      runnerSettings,
      configuration,
                                 project,
                                 projectJdk);
  }
}
