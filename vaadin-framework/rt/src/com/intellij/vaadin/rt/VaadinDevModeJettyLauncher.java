package com.intellij.vaadin.rt;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.jetty.JettyLauncher;

import java.io.File;

public class VaadinDevModeJettyLauncher extends JettyLauncher {
  private String myWarPath;

  @Override
  public boolean processArguments(TreeLogger logger, String arguments) {
    myWarPath = arguments;
    return true;
  }

  @Override
  public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws Exception {
    File root = myWarPath != null ? new File(myWarPath) : appRootDir;
    return super.start(logger, port, root);
  }
}
