package com.intellij.gwt.gradle;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtGradleSdkPaths;

import java.io.File;

import static org.jetbrains.jps.gwt.model.GwtSdkPaths.isGwtMavenGroupId;

public final class GwtGradleSdkType extends GwtSdkType {
  public GwtGradleSdkType() {
    super(GwtGradleSdkPaths.TYPE_ID);
  }

  @Override
  public @NotNull GwtSdk createSdk(String homeDirectoryUrl) {
    final String homePath = VfsUtilCore.urlToPath(homeDirectoryUrl);
    return new GwtGradleSdk(homePath, getVersion(homePath));
  }

  @Override
  public boolean isValidSdkHomeDirectory(File directory) {
    File gwtDev = directory.getParentFile();
    if (gwtDev == null || !gwtDev.getName().equals(GwtSdkPaths.GWT_DEV_ARTIFACT_ID)) return false;

    File base = gwtDev.getParentFile();
    return base != null && isGwtMavenGroupId(base.getName());
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  private static String getVersion(String homePath) {
    return PathUtil.getFileName(homePath);
  }

  @Override
  public String getPresentableName(String sdkPath) {
    return GwtBundle.message("label.text.gwt.0.sdk.from.gradle.repository", getVersion(sdkPath));
  }
}
