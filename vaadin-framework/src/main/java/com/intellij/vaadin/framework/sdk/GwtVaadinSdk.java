package com.intellij.vaadin.framework.sdk;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtSdkBase;
import com.intellij.openapi.roots.libraries.JarVersionDetectionUtil;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;

import java.io.File;

public class GwtVaadinSdk extends GwtSdkBase {
  public GwtVaadinSdk(GwtSdkPaths paths) {
    super(paths);
  }

  @NotNull
  @Override
  protected GwtVersion detectVersion() {
    return VaadinVersionUtil.getGwtVersion(JarVersionDetectionUtil.getImplementationVersion(new File(myPaths.getDevJarPath(true))));
  }

  @Override
  public boolean isValid() {
    return getUserJar() != null;
  }
}
