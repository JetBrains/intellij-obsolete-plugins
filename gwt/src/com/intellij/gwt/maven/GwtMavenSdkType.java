package com.intellij.gwt.maven;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtMavenSdkPaths;

import java.io.File;

public final class GwtMavenSdkType extends GwtSdkType {
  public GwtMavenSdkType() {
    super(GwtMavenSdkPaths.TYPE_ID);
  }

  @Override
  public @NotNull GwtSdk createSdk(String homeDirectoryUrl) {
    final String homePath = VfsUtilCore.urlToPath(homeDirectoryUrl);
    return new GwtMavenSdk(homePath, getVersion(homePath));
  }

  @Override
  public boolean isValidSdkHomeDirectory(File directory) {
    File gwtDev = directory.getParentFile();
    if (gwtDev == null) return false;

    File base = gwtDev.getParentFile();
    if (base == null) return false;

    File parent = base.getParentFile();
    if (base.getName().equals("gwt")
        && parent != null && parent.getName().equals("google")
        && parent.getParentFile() != null && parent.getParentFile().getName().equals("com")) {
      return true;
    }

    return base.getName().equals("gwtproject")
           && parent != null && parent.getName().equals("org");
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
    return GwtBundle.message("label.gwt.0.sdk.from.maven.repository", getVersion(sdkPath));
  }
}
