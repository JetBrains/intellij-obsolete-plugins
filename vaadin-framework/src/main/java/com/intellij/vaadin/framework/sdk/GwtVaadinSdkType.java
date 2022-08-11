package com.intellij.vaadin.framework.sdk;

import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinSdkPaths;

import java.io.File;

public class GwtVaadinSdkType extends GwtSdkType {
  public GwtVaadinSdkType() {
    super(GwtVaadinSdkPaths.TYPE_ID);
  }

  @NotNull
  @Override
  public GwtSdk createSdk(String homeDirectoryUrl) {
    return new GwtVaadinSdk(new GwtVaadinSdkPaths(VfsUtilCore.urlToPath(homeDirectoryUrl)));
  }

  @Override
  public boolean isValidSdkHomeDirectory(File directory) {
    File[] children = directory.listFiles();
    boolean clientFound = false, serverFound = false;
    if (children != null) {
      for (File file : children) {
        String fileName = file.getName();
        if (fileName.endsWith(".jar")) {
          serverFound |= fileName.startsWith("vaadin-server-");
          clientFound |= fileName.startsWith("vaadin-client-");
        }
      }
    }
    return serverFound && clientFound;
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  @Override
  public String getPresentableName(String sdkPath) {
    return "GWT from Vaadin installation '" + sdkPath + "'";
  }
}
