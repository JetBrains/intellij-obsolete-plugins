package com.intellij.gwt.sdk.impl;

import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.PropertiesUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtSdkPathUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.intellij.util.text.VersionComparatorUtil.compare;

public final class GwtVersionDetector {
  private static final Logger LOG = Logger.getInstance(GwtVersionDetector.class);

  public static @NotNull GwtVersion detectGwtVersion(@NotNull String sdkHomePath) {
    if (StringUtil.isEmptyOrSpaces(sdkHomePath) || !new File(sdkHomePath).exists()) {
      return GwtVersionImpl.getDefaultVersion();
    }

    String devJarPath = GwtSdkPathUtil.getSystemIndependentDevJarPath(sdkHomePath);
    if (!new File(devJarPath).exists()) {
      devJarPath = GwtSdkPathUtil.getSystemDependentDevJarPath(sdkHomePath);
    }

    VirtualFile devJar = JarFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(devJarPath) + JarFileSystem.JAR_SEPARATOR);
    if (devJar != null) {
      VirtualFile propertiesFile = devJar.findFileByRelativePath("com/google/gwt/dev/About.properties");
      if (propertiesFile != null) {
        try (InputStreamReader reader = new InputStreamReader(propertiesFile.getInputStream(), StandardCharsets.UTF_8)) {
          Map<String, String> properties = PropertiesUtil.loadProperties(reader);
          String versionString = properties.get("gwt.version");
          if (versionString == null) {
            LOG.info("Cannot find gwt.version property in " + propertiesFile.getUrl());
            return GwtVersionImpl.getDefaultVersion();
          }
          GwtVersionImpl version = getGwtVersionFromString(versionString);
          if (!version.isAtLeast(GwtVersionImpl.VERSION_1_6)) {
            LOG.info(
              "Suspicious gwt.version property (" + versionString + ") in " + propertiesFile.getUrl() + ", using default version instead");
            return GwtVersionImpl.getDefaultVersion();
          }
          return version;
        }
        catch (IOException e) {
          LOG.info("Cannot load properties from " + propertiesFile.getUrl(), e);
          return GwtVersionImpl.getDefaultVersion();
        }
      }
    }

    VirtualFile userJar = GwtSdkUtil.getUserJar(sdkHomePath);
    if (userJar != null) {
      VirtualFile[] files = {userJar};
      if (!LibraryUtil.isClassAvailableInLibrary(files, GwtI18nUtil.CONSTANTS_INTERFACE_NAME)) {
        return GwtVersionImpl.VERSION_1_0;
      }
      if (userJar.findFileByRelativePath(GwtSdkUtil.getJreEmulationClassPath(Iterable.class.getName())) != null) {
        return GwtVersionImpl.VERSION_1_5;
      }
      if (userJar.findFileByRelativePath(GwtSdkUtil.getJreEmulationClassPath(Serializable.class.getName())) != null) {
        return GwtVersionImpl.VERSION_1_4;
      }
    }

    return GwtVersionImpl.VERSION_FROM_1_1_TO_1_3;
  }

  public static GwtVersionImpl getGwtVersionFromString(String version) {
      if (compare(version, "2.8.snapshot") >= 0) {
        return GwtVersionImpl.VERSION_2_8;
      }
      if (compare(version, "2.7.snapshot") >= 0) {
        return GwtVersionImpl.VERSION_2_7;
      }
      else if (compare(version, "2.6") >= 0) {
        return GwtVersionImpl.VERSION_2_6;
      }
      else if (compare(version, "2.5") >= 0) {
        return GwtVersionImpl.VERSION_2_5;
      }
      else if (compare(version, "2.4") >= 0) {
        return GwtVersionImpl.VERSION_2_4;
      }
      else if (compare(version, "2.0") >= 0) {
        return GwtVersionImpl.VERSION_2_0;
      }
      else if (compare(version, "1.6") >= 0) {
        return GwtVersionImpl.VERSION_1_6;
      }
      else if (compare(version, "1.5") >= 0) {
        return GwtVersionImpl.VERSION_1_5;
      }
      else if (compare(version, "1.4") >= 0) {
        return GwtVersionImpl.VERSION_1_4;
      }
      else if (compare(version, "1.1") >= 0) {
        return GwtVersionImpl.VERSION_FROM_1_1_TO_1_3;
      }
      else if (compare(version, "1.0") <= 0) {
        return GwtVersionImpl.VERSION_1_0;
      }
      return GwtVersionImpl.getDefaultVersion();
    }
}
