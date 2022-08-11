package com.intellij.vaadin.debug;

import com.intellij.gwt.run.GwtDevModeServer;
import com.intellij.gwt.run.GwtDevModeServerProvider;

import java.util.Collections;
import java.util.List;

public class VaadinDevModeServerProvider extends GwtDevModeServerProvider {
  @Override
  public List<? extends GwtDevModeServer> getServers() {
    return Collections.singletonList(new VaadinDevModeServer());
  }
}
