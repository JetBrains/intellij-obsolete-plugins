package com.intellij.dmserver.integration;

import org.jetbrains.annotations.NotNull;

public class DMServerIntegrationStateBase {

  private static final int DEFAULT_DMSHELL_PORT = 2401;
  private static final int DEFAULT_DEPLOYMENT_TIMEOUT = 30;

  private String myInstallationHome = "";
  private boolean myShellEnabled = true;
  private int myShellPort = DEFAULT_DMSHELL_PORT;
  private int myDeploymentTimeoutSecs = DEFAULT_DEPLOYMENT_TIMEOUT;
  private String myPickupFolder = "";
  private String myDumpsFolder = "";
  private boolean myWrapSystemOut = true;
  private boolean myWrapSystemErr = true;

  @NotNull
  public String getInstallationHome() {
    return myInstallationHome;
  }

  public void setInstallationHome(@NotNull String installationHome) {
    myInstallationHome = installationHome;
  }

  public int getShellPort() {
    return myShellPort;
  }

  public void setShellPort(int shellPort) {
    myShellPort = shellPort;
  }

  public boolean isShellEnabled() {
    return myShellEnabled;
  }

  public void setShellEnabled(boolean shellEnabled) {
    myShellEnabled = shellEnabled;
  }

  public int getDeploymentTimeoutSecs() {
    return myDeploymentTimeoutSecs;
  }

  public void setDeploymentTimeoutSecs(int deploymentTimeoutSecs) {
    myDeploymentTimeoutSecs = deploymentTimeoutSecs;
  }

  public String getPickupFolder() {
    return myPickupFolder;
  }

  public void setPickupFolder(String pickupFolder) {
    myPickupFolder = pickupFolder;
  }

  public String getDumpsFolder() {
    return myDumpsFolder;
  }

  public void setDumpsFolder(String dumpsFolder) {
    myDumpsFolder = dumpsFolder;
  }

  public boolean isWrapSystemErr() {
    return myWrapSystemErr;
  }

  public void setWrapSystemErr(boolean wrapSystemErr) {
    myWrapSystemErr = wrapSystemErr;
  }

  public boolean isWrapSystemOut() {
    return myWrapSystemOut;
  }

  public void setWrapSystemOut(boolean wrapSystemOut) {
    myWrapSystemOut = wrapSystemOut;
  }
}
