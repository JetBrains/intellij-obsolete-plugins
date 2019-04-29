/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.j2meplugin.emulator.ui.MobileApiSettingsEditor;
import com.intellij.j2meplugin.module.settings.midp.MIDPApplicationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public abstract class MIDPEmulatorType extends EmulatorType {
  protected static final String SDK_NAME = "_SDK_NAME";
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");

  @Override
  @NonNls
  public String getApplicationType() {
    return MIDPApplicationType.NAME;
  }

  @Override
  @NonNls
  @Nullable
  public String getDescriptorOption() {
    return "-Xdescriptor:";
  }

  @Override
  @Nullable
  @NonNls
  public String getDeviceOption() {
    return "-Xdevice:";
  }

  @Override
  @Nullable
  @NonNls
  public String getRelativePathToEmulator() {
    return "bin/emulator";
  }

  protected Properties getVersionProperties(String home) {
    String emulatorExe = getPathToEmulator(home);

    if (emulatorExe == null || emulatorExe.length() < 1) {
      return null;
    }
    String versionOutput = getExeOutput(emulatorExe, "-version");
    if (versionOutput == null || versionOutput.length() < 4) {
      return null;
    }
    return convertVersionOutputToProperties(versionOutput);
  }

  static Properties convertVersionOutputToProperties(String versionOutput) {
    BufferedReader versionOutputLines = new BufferedReader(new StringReader(versionOutput), 512);
    String line;
    int lineNumber = 0;
    Properties props = new Properties();
    while ( (line = readLine(versionOutputLines)) != null) {
      if (lineNumber == 1) {
        props.setProperty(SDK_NAME, line);
      } else if (lineNumber > 1) {
        int colon = line.indexOf(':');
        if (colon > 0 && (colon+1) < line.length()) {
          String key = line.substring(0, colon);
          String value = line.substring(colon + 1).trim();
          props.setProperty(key, value);
        }
      }
      ++lineNumber;
    }
    return props;
  }

  @Nullable
  private static String readLine(BufferedReader br) {
    try {
      return br.readLine();
    } catch (IOException ignored) {
      return null;
    }
  }

  @Nullable
  protected static String getExeOutput(String exe, String param) {
     final StringBuffer output = new StringBuffer();
     if (exe == null || exe.length() < 1) {
         return null;
     }
     GeneralCommandLine generalCommandLine = new GeneralCommandLine();
     generalCommandLine.setExePath(exe);
     generalCommandLine.addParameter(param);
     try {
        OSProcessHandler osProcessHandler = new OSProcessHandler(generalCommandLine);
        osProcessHandler.addProcessListener(new ProcessAdapter() {
          @Override
          public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            output.append(event.getText());
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
      return output.toString();
  }

  @Override
  @Nullable
  public String suggestName(String homePath) {
    Properties versionProps = getVersionProperties(homePath);
    if (versionProps == null) {
      return null;
    }
    return versionProps.getProperty(SDK_NAME);
  }

  @NonNls
  public abstract String getDefaultProfile(@NotNull String home);

  public String[] getAvailableProfiles(@NotNull String homePath) {
    return new String[] {getDefaultProfile(homePath)};
  }

  @NonNls
  public abstract String getDefaultConfiguration(@NotNull String home);

  public String[] getAvailableConfigurations(@NotNull String homePath) {
    return new String[] {getDefaultConfiguration(homePath)};
  }

  public abstract MobileApiSettingsEditor getApiEditor(final String homePath, Sdk sdk, SdkModificator sdkModificator);
}
