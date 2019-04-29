/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public abstract class EmulatorType implements PluginAware{
  public static final ExtensionPointName<EmulatorType> EMULATOR_TYPE_EXTENSION = ExtensionPointName.create("J2ME.com.intellij.emulatorType");

  @Override
  public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
  }

  @NonNls
  public abstract String getName();

  @Nullable
  @NonNls
  public String getPreverifyPath() {
    return "bin/preverify";
  }

  @NonNls
  public abstract String getApplicationType();

  @NonNls
  @Nullable
  public abstract String getDescriptorOption();

  @Nullable
  @NonNls
  public abstract String getDeviceOption();

  @Nullable
  @NonNls
  public abstract String getRelativePathToEmulator();

  @Nullable
  public String[] getOTACommands() {
    return null;
  }

  /**
   * Cashed in emulator
   */
  @Nullable
  public String [] getOTACommands(String home) {
    return getOTACommands();
  }

  @Nullable
  @NonNls
  public String getPrefPath(String home) {
    return null;
  }

  @Nullable
  @NonNls
  public String getUtilPath(String home) {
    return null;
  }

  @Nullable
  @NonNls
  public String getPreverifyPath(String home) {
    return toSystemDependentPath(home, getPreverifyPath());
  }

  @Nullable
  protected static String toSystemDependentPath(String home, @NonNls String path) {
    if (path == null) return null;
    return FileUtil.toSystemDependentName(home + "/" + path);
  }

  @Nullable
  @NonNls
  public String getPathToEmulator(String home) {
    return toSystemDependentPath(home, getRelativePathToEmulator());
  }

  @Nullable
  public String[] getApi(String homePath) {
    return null;
  }

  @Nullable
  public abstract String suggestName(String homePath);

  public abstract boolean isValidHomeDirectory(String homePath);

  @Nullable
  @NonNls
  public abstract String [] getAvailableSkins(String homePath);

  public abstract J2MERunnableState getJ2MERunnableState(Executor executor,
                                                         RunnerSettings runnerSettings,
                                                         J2MERunConfiguration configuration,
                                                         Project project,
                                                         Sdk projectJdk);
}
