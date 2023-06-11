package com.intellij.tcserver.server.integration;

import com.intellij.javaee.appServers.appServerIntegrations.AppServerDeployedFileUrlProvider;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerHelper;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.openapi.ex.AppServerIntegrationsManager;
import com.intellij.tcserver.deployment.TcServerDeploymentProvider;
import com.intellij.tcserver.util.TcServerBundle;
import icons.JavaeeAppServersTcServerIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TcServerIntegration extends AppServerIntegration {
  private final TcServerHelper myServerHelper;

  public static TcServerIntegration getInstance() {
    return AppServerIntegrationsManager.getInstance().getIntegration(TcServerIntegration.class);
  }

  public TcServerIntegration() {
    myServerHelper = new TcServerHelper();
  }

  @Override
  public Icon getIcon() {
    return JavaeeAppServersTcServerIcons.Browser_logo_sts_16;
  }

  @Override
  public String getPresentableName() {
    return TcServerBundle.message("spring_tc_server");
  }

  @Override
  public ApplicationServerHelper getApplicationServerHelper() {
    return myServerHelper;
  }

  @Override
  public DeploymentProvider getDeploymentProvider(boolean local) {
    return new TcServerDeploymentProvider();
  }

  @Override
  public
  @NotNull
  AppServerDeployedFileUrlProvider getDeployedFileUrlProvider() {
    return TcServerUrlMapping.getInstance();
  }
}
