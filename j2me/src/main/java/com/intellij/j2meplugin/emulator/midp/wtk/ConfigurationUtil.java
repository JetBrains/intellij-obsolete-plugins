/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp.wtk;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.JarUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author anna
 */
public class ConfigurationUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin.emulator.midp.wtk.ConfigurationUtil");

  private static final String TITLE_VERSIONED_PROPERTY = "TITLE_VERSIONED";

  private ConfigurationUtil() { }

  @Nullable
  private static Properties getWTKEmulatorProperties(String homeDir) {
    return JarUtil.loadProperties(new File(homeDir, "wtklib/ktools.zip"), "I18N.properties");
  }

  @Nullable
  public static Properties getApiSettings(String homeDir) {
    return JarUtil.loadProperties(new File(homeDir, "wtklib/ktools.zip"), "com/sun/kvem/toolbar/ApiSettings.properties");
  }

  @Nullable
  @SuppressWarnings({"HardCodedStringLiteral"})
  public static String[] getDefaultApiPath(String homePath) {
    final Properties apiSettings = getApiSettings(homePath);
    if (apiSettings == null || apiSettings.isEmpty()) {
      return null;
    }
    final String defaultConf = apiSettings.getProperty("default");
    final String[] apiPaths = defaultConf != null ? defaultConf.split(", ") : null;
    if (apiPaths == null) {
      return null;
    }
    final String optionalConf = apiSettings.getProperty("optional");
    final String [] optionalPaths = optionalConf != null ? optionalConf.split(", ") : null;
    ArrayList<String> jars = new ArrayList<>();
    for (String apiPath : apiPaths) {
      if (optionalPaths != null && ArrayUtil.find(optionalPaths, apiPath) == -1){
        String jar = apiSettings.getProperty(apiPath + ".file");
        if (jar != null) {
          jars.add(homePath + File.separator + "lib" + File.separator + jar);
        }
      }
    }
    return ArrayUtil.toStringArray(jars);
  }

  @Nullable
  public static String getWTKVersion(String homeDir) {
    final Properties emulatorProperties = getWTKEmulatorProperties(homeDir);
    return emulatorProperties != null ? emulatorProperties.getProperty(TITLE_VERSIONED_PROPERTY) : null;
  }

  private static String getWTKConfigurations(@NotNull final String homeDir, @NonNls final String name) {
    File file = new File(homeDir, "lib/system.config");
    if (file.canRead()) {
      try {
        InputStream stream = new FileInputStream(file);
        try {
          Properties properties = new Properties();
          properties.load(stream);
          return properties.getProperty(name);
        }
        finally {
          stream.close();
        }
      }
      catch (IOException e) {
        LOG.debug(e);
      }
    }

    return null;
  }

  @Nullable
  public static String getProfileVersion(@NotNull String homeDir) {
    return getWTKConfigurations(homeDir, "microedition.profiles");
  }

  @Nullable
  public static String getConfigurationVersion(@NotNull String homeDir) {
    return getWTKConfigurations(homeDir, "microedition.configuration");
  }

  @Nullable
  @SuppressWarnings({"HardCodedStringLiteral"})
  public static String[] getWTKDevices(String homeDir) {
    ArrayList<String> result = new ArrayList<>();
    final VirtualFile homeDirectory = LocalFileSystem.getInstance().findFileByPath(homeDir);
    if (homeDirectory == null) {
      return null;
    }
    VirtualFile devicesDirectory = homeDirectory.findFileByRelativePath("wtklib/devices");
    if (devicesDirectory == null) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }
    VirtualFile[] devices = devicesDirectory.getChildren();
    for (int i = 0; devices != null && i < devices.length; i++) {
      if (devices[i].isDirectory() && devices[i].findChild(devices[i].getName() + ".properties") != null) {
        result.add(devices[i].getName());
      }
    }
    return ArrayUtil.toStringArray(result);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static boolean isValidWTKHome(String homeDir) {
    final Properties wtkEmulatorProperties = getWTKEmulatorProperties(homeDir);
    String property = wtkEmulatorProperties != null ? wtkEmulatorProperties.getProperty("TITLE_PROJECT") : null;
    return property != null && property.contains("Toolkit");
  }
}
