package com.intellij.vaadin.maven;

import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinMavenSdkPaths;

import java.io.File;

public class GwtVaadinMavenSdkType extends GwtSdkType {
  public GwtVaadinMavenSdkType() {
    super(GwtVaadinMavenSdkPaths.TYPE_ID);
  }

  @NotNull
  @Override
  public GwtSdk createSdk(String homeDirectoryUrl) {
    return new GwtVaadinMavenSdk(VfsUtilCore.urlToPath(homeDirectoryUrl));
  }

  @Override
  public boolean isValidSdkHomeDirectory(File directory) {
    File vaadinClientDir = directory.getParentFile();
    if (vaadinClientDir == null || !vaadinClientDir.getName().equals("vaadin-client")) return false;

    File vaadinBase = vaadinClientDir.getParentFile();
    if (vaadinBase == null || !vaadinBase.getName().equals("vaadin")) return false;

    File parent = vaadinBase.getParentFile();
    return parent != null && parent.getName().equals("com");
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  @Override
  public String getPresentableName(String sdkPath) {
    return "GWT from Vaadin '" + GwtVaadinMavenSdkPaths.getVersion(sdkPath) + "' in Maven Repository";
  }
}
