package com.intellij.gwt.run;

import java.util.Collections;
import java.util.List;

public final class DefaultDevModeServerProvider extends GwtDevModeServerProvider {
  @Override
  public List<? extends GwtDevModeServer> getServers() {
    return Collections.singletonList(new DefaultDevModeServer());
  }
}
