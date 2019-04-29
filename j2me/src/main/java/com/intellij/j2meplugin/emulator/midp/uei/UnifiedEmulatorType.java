/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp.uei;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.j2meplugin.emulator.midp.MIDPEmulatorType;
import com.intellij.j2meplugin.emulator.ui.MobileApiSettingsEditor;
import com.intellij.j2meplugin.emulator.ui.MobileDefaultApiEditor;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.j2meplugin.run.states.midp.uei.UEIRunnableState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.Key;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

public class UnifiedEmulatorType extends MIDPEmulatorType {
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");
  private String myProfile;
  private String myConfiguration;
  @NonNls
  public static final String INSTALL = "install";
  @NonNls
  public static final String FORCE = "force";
  @NonNls
  public static final String RUN = "run";
  @NonNls
  public static final String REMOVE = "remove";
  @NonNls
  public static final String TRANSIENT = "transient";
  @NonNls
  public static final String STORAGE_NAMES = "storageNames";

  @Override
  @NonNls
  public String getName() {
    return "Unified Emulator Type";
  }

  @Override
  public boolean isValidHomeDirectory(String homePath) {
    return fillEmulatorConfigurations(homePath);
  }

  @Override
  public J2MERunnableState getJ2MERunnableState(Executor executor,
                                                RunnerSettings runnerSettings,
                                                J2MERunConfiguration configuration,
                                                Project project,
                                                Sdk projectJdk) {
    return new UEIRunnableState(
      runnerSettings,
      configuration,
                                project,
                                projectJdk);
  }

  @Override
  public String getDefaultProfile(@NotNull String home) {
    if (myProfile == null || myProfile.length() == 0) {
      fillEmulatorConfigurations(home);
    }
    return myProfile;
  }

  @Override
  public String getDefaultConfiguration(@NotNull String home) {
    if (myConfiguration == null || myConfiguration.length() == 0) {
      fillEmulatorConfigurations(home);
    }
    return myConfiguration;
  }

  @Override
  public MobileApiSettingsEditor getApiEditor(final String homePath, Sdk sdk, SdkModificator sdkModificator) {
    return new MobileDefaultApiEditor();
  }

  @Override
  public String[] getAvailableSkins(final String homePath) {
    String exe = getPathToEmulator(homePath);
    return exe != null ? fillEmulatorDevices(exe) : null;
  }

  private boolean fillEmulatorConfigurations(String home) {
    Properties versionProps = getVersionProperties(home);
    if (versionProps == null) {
      return false;
    }
    myProfile = versionProps.getProperty("Profile");
    myConfiguration = versionProps.getProperty("Configuration");
    String suggestedName = versionProps.getProperty(SDK_NAME);
    return (myProfile != null && myConfiguration != null && suggestedName != null);
  }

  @Nullable
  public static String[] fillEmulatorDevices(String exe) {
    final StringBuffer help = new StringBuffer();
    if (exe != null && exe.length() > 0) {
      GeneralCommandLine generalCommandLine = new GeneralCommandLine();
      generalCommandLine.setExePath(exe);
      @NonNls final String query = "-Xquery";
      generalCommandLine.addParameter(query);
      try {
        OSProcessHandler osProcessHandler = new OSProcessHandler(generalCommandLine);
        osProcessHandler.addProcessListener(new ProcessAdapter() {
          @Override
          public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            help.append(event.getText());
          }
        });
        osProcessHandler.startNotify();
        osProcessHandler.waitFor();
        osProcessHandler.destroyProcess();
      }
      catch (ExecutionException e) {
        LOG.debug(e);
        return null;
      }
      Properties properties = new Properties();
      try {
        properties.load(new StringReader(help.toString()));
        @NonNls final String key = "device.list";
        final String devices = properties.getProperty(key);
        if (devices != null) {
          final String[] skins = devices.split(",");
          for (int i = 0; i < skins.length; i++) {
            skins[i] = skins[i].trim();
          }
          return skins;
        }
        else {
          return null;
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
    return null;
  }

  @Override
  @Nullable
  public String[] getOTACommands(String homeDir) {
    String exe = getPathToEmulator(homeDir);
    if (exe != null && exe.length() != 0) {
      GeneralCommandLine generalCommandLine = new GeneralCommandLine();
      generalCommandLine.setExePath(exe);
      generalCommandLine.addParameter("-help");
      try {
        OSProcessHandler osProcessHandler = new OSProcessHandler(generalCommandLine);
        @NonNls final StringBuffer buffer = new StringBuffer();
        osProcessHandler.addProcessListener(new ProcessAdapter() {
          @Override
          public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            buffer.append(event.getText());
          }
        });
        osProcessHandler.startNotify();
        osProcessHandler.waitFor();
        osProcessHandler.destroyProcess();
        if (buffer.length() != 0 && buffer.indexOf("-Xjam") != -1) {
          @NonNls String otaCommands = buffer.substring(buffer.indexOf("-Xjam") + "-Xjam".length());
          final int endIndex = otaCommands.indexOf("-X");
          if (endIndex > -1) {
            otaCommands = otaCommands.substring(0, endIndex);
          }
          ArrayList<String> result = new ArrayList<>();
          if (otaCommands.contains(INSTALL)) result.add(INSTALL);
          if (otaCommands.contains(FORCE)) result.add(FORCE);
          if (otaCommands.contains(RUN)) result.add(RUN);
          if (otaCommands.contains(REMOVE)) result.add(REMOVE);
          if (otaCommands.contains(TRANSIENT)) result.add(TRANSIENT);
          if (otaCommands.contains(STORAGE_NAMES)) result.add(STORAGE_NAMES);
          return ArrayUtil.toStringArray(result);
        }
      }
      catch (ExecutionException e) {
        return null;
      }
    }
    return null;
  }
}
