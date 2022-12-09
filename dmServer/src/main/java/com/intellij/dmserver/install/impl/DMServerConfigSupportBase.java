package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.install.DMServerConfigSupport;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public abstract class DMServerConfigSupportBase implements DMServerConfigSupport {

  protected static final ConfigElementWrapper<String> ourDumpsFolderWrapper = new PathElementWrapper() {
    @Override
    protected String doGetValue(DMServerIntegrationData data) {
      return data.getDumpsFolder();
    }

    @Override
    protected void doSetValue(DMServerIntegrationData data, String value) {
      data.setDumpsFolder(value);
    }
  };

  protected static final ConfigElementWrapper<Boolean> ourShellEnabledWrapper = new ConfigElementWrapper<>() {
    @Override
    public Boolean getValue(DMServerIntegrationData data) {
      return data.isShellEnabled();
    }

    @Override
    public void setValue(DMServerIntegrationData data, Boolean value) {
      data.setShellEnabled(value);
    }
  };

  protected static final ConfigElementWrapper<Integer> ourShellPortWrapper = new ConfigElementWrapper<>() {
    @Override
    public Integer getValue(DMServerIntegrationData data) {
      return data.getShellPort();
    }

    @Override
    public void setValue(DMServerIntegrationData data, Integer value) {
      data.setShellPort(value);
    }
  };

  protected static final ConfigElementWrapper<String> ourPickupFolderWrapper = new PathElementWrapper() {
    @Override
    protected String doGetValue(DMServerIntegrationData data) {
      return data.getPickupFolder();
    }

    @Override
    protected void doSetValue(DMServerIntegrationData data, String value) {
      data.setPickupFolder(value);
    }
  };

  protected static final ConfigElementWrapper<Integer> ourDeploymentTimeoutSecsWrapper = new ConfigElementWrapper<>() {
    @Override
    public Integer getValue(DMServerIntegrationData data) {
      return data.getDeploymentTimeoutSecs();
    }

    @Override
    public void setValue(DMServerIntegrationData data, Integer value) {
      data.setDeploymentTimeoutSecs(value);
    }
  };

  private static abstract class PathElementWrapper implements ConfigElementWrapper<String> {

    @Override
    public String getValue(DMServerIntegrationData data) {
      return FileUtil.toSystemDependentName(doGetValue(data));
    }

    @Override
    public void setValue(DMServerIntegrationData data, String value) {
      doSetValue(data, FileUtil.toSystemIndependentName(value));
    }

    protected abstract String doGetValue(DMServerIntegrationData data);

    protected abstract void doSetValue(DMServerIntegrationData data, String value);
  }

  @Override
  public boolean isValid() {
    for (VirtualFile configFile : getFiles()) {
      if (!DMServerInstallationImpl.isValidFile(configFile)) {
        return false;
      }
    }
    return true;
  }

  protected abstract List<VirtualFile> getFiles();
}
