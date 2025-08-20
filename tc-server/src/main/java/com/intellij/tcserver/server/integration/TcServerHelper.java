package com.intellij.tcserver.server.integration;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.appServerIntegrations.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.util.TcServerBundle;

import java.io.File;

class TcServerHelper implements ApplicationServerHelper {
  private static final Logger MY_LOG = Logger.getInstance(TcServerHelper.class);

  @Override
  public ApplicationServerInfo getApplicationServerInfo(ApplicationServerPersistentData persistentData)
    throws CantFindApplicationServerJarsException {
    File[] libs = new File[0];
    String serverName = TcServerBundle.message("tc_server");
    if (persistentData != null) {
      final TcServerData data = (TcServerData)persistentData;
      try {
        libs = TcServerUtil.getLibraries(data.getSdkPath(), data.getServerName());
      }
      catch (RuntimeConfigurationException e) {
        MY_LOG.debug(e);
        //silently
      }
      String dataServerName = data.getServerName();
      if (!StringUtil.isEmpty(dataServerName)) {
        serverName = dataServerName + " (" + serverName + ")";
      }
    }

    File[] libCopy = libs.clone();

    return new ApplicationServerInfo(libCopy, serverName);
  }

  @Override
  public ApplicationServerPersistentData createPersistentDataEmptyInstance() {
    return new TcServerData("", "");
  }

  @Override
  public ApplicationServerPersistentDataEditor createConfigurable() {
    return new TcServerEditor();
  }
}
