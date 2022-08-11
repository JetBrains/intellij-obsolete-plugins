package com.intellij.vaadin.debug;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.run.GwtDevModeServer;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PathUtil;
import com.intellij.vaadin.framework.VaadinConstants;
import com.intellij.vaadin.VaadinIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

public final class VaadinDevModeServer extends GwtDevModeServer {
  private static final Logger LOG = Logger.getInstance(VaadinDevModeServer.class);

  public VaadinDevModeServer() {
    super("vaadin-jetty-launcher", "Vaadin Jetty Launcher");
  }

  @Override
  public @NotNull Icon getIcon() {
    return VaadinIcons.VaadinIcon;
  }

  @Override
  public void patchParameters(@NotNull JavaParameters parameters, String originalOutputDir, @NotNull GwtFacet gwtFacet) {
    ParametersList params = parameters.getProgramParametersList();
    params.add("-server");
    params.add("com.intellij.vaadin.rt.VaadinDevModeJettyLauncher:" + originalOutputDir);
    File vaadinLibDir;
    File vaadinClassesRoot = new File(PathUtil.getJarPathForClass(getClass()));
    if (vaadinClassesRoot.isDirectory()) {
      vaadinLibDir = new File(PathManager.getHomePath(), "plugins/vaadin/rt/lib");//dev mode
    }
    else {
      vaadinLibDir = new File(vaadinClassesRoot.getParentFile(), "rt");
    }
    File vaadinJettyLauncherJar = new File(vaadinLibDir, "vaadin-jetty-launcher.jar");
    LOG.assertTrue(vaadinJettyLauncherJar.exists(), vaadinJettyLauncherJar + " doesn't exist");
    parameters.getClassPath().add(vaadinJettyLauncherJar);
  }

  @NotNull
  @Override
  public String patchWarDirectoryPath(@NotNull String warDirectoryPath) {
    return new File(warDirectoryPath, VaadinConstants.VAADIN_WIDGET_SETS_PATH).getAbsolutePath();
  }
}
