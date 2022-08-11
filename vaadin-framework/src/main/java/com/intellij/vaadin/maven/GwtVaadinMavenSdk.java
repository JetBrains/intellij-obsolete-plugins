package com.intellij.vaadin.maven;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtSdkBase;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinMavenSdkPaths;

public class GwtVaadinMavenSdk extends GwtSdkBase {
  public GwtVaadinMavenSdk(String clientDirectoryPath) {
    super(new GwtVaadinMavenSdkPaths(clientDirectoryPath));
  }

  @NotNull
  @Override
  protected GwtVersion detectVersion() {
    return VaadinVersionUtil.getGwtVersion(((GwtVaadinMavenSdkPaths)myPaths).getVersion());
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
