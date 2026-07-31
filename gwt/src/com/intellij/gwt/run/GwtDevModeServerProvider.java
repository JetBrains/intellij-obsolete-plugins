package com.intellij.gwt.run;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.List;

public abstract class GwtDevModeServerProvider {
  public static final ExtensionPointName<GwtDevModeServerProvider> EP_NAME = ExtensionPointName.create("com.intellij.gwt.devModeServerProvider");

  public abstract List<? extends GwtDevModeServer> getServers();

}
