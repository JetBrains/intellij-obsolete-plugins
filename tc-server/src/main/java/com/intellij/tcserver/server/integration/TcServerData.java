package com.intellij.tcserver.server.integration;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public final class TcServerData implements ApplicationServerPersistentData {
  private String mySdkPath;
  private String myServerName;
  private List<String> myAvailableServers = new LinkedList<>();
  private Integer myJmxPort;
  private Integer myHttpPort;
  private TcServerVersion myVersion;

  public TcServerData(@NotNull String sdkPath, @NotNull String serverName) {
    mySdkPath = sdkPath;
    myServerName = serverName;
  }

  @NotNull
  public String getSdkPath() {
    return mySdkPath;
  }

  @NotNull
  @NlsSafe
  public String getServerName() {
    return myServerName;
  }

  /* @NotNull*/

  public List<String> getAvailableServers() {
    return myAvailableServers;
  }

  public void setSdkPath(@NotNull String sdkPath) {
    mySdkPath = sdkPath;
  }

  public void setServerName(@NotNull String serverName) {
    myServerName = serverName;
  }

  public void setAvailableServers(@NotNull List<String> availableServers) {
    myAvailableServers = availableServers;
  }

  public Integer getJmxPort() {
    return myJmxPort;
  }

  public void setJmxPort(Integer jmxPort) {
    myJmxPort = jmxPort;
  }

  public Integer getHttpPort() {
    return myHttpPort;
  }

  public void setHttpPort(Integer httpPort) {
    myHttpPort = httpPort;
  }

  public TcServerVersion getVersion() {
    return myVersion;
  }

  public void setVersion(TcServerVersion version) {
    myVersion = version;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    final TcServerData data = new TcServerData("", "");
    XmlSerializer.deserializeInto(data, element);

    //  no validation:       invalid configuration should also be saved

    //todo remove toSystemIndependentName call later. It is needed only to fix incorrect config files
    mySdkPath = FileUtil.toSystemIndependentName(data.getSdkPath());
    myServerName = data.getServerName();
    myAvailableServers = data.getAvailableServers();
    myJmxPort = data.getJmxPort();
    myHttpPort = data.getHttpPort();
    myVersion = data.getVersion();
    if (myVersion == null) { //there was no version in old plugin
      myVersion = TcServerVersion.BEFORE_2_1;
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    TcServerData data = new TcServerData(mySdkPath, myServerName);
    data.setAvailableServers(myAvailableServers);
    data.setJmxPort(myJmxPort);
    data.setHttpPort(myHttpPort);
    data.setVersion(myVersion);
    XmlSerializer.serializeInto(data, element, null);
    //  no validation:       invalid configuration should also be red
  }

  public static void validateTcServerData(String sdkPath, String serverName, List<String> availableServers)
    throws RuntimeConfigurationException {
    TcServerUtil.validateSdkPath(sdkPath);

    if (availableServers == null || availableServers.isEmpty()) {
      throw new RuntimeConfigurationError(TcServerBundle.message("validation.emptyAvailableServices"));
    }

    if (serverName == null || serverName.isEmpty()) {
      throw new RuntimeConfigurationError(TcServerBundle.message("validation.emptySelectedServiceName"));
    }

    if (!availableServers.contains(serverName)) {
      throw new RuntimeConfigurationError(TcServerBundle.message("validation.nonExistingServerName", serverName));
    }
  }


  public static void validatePorts(Integer jmxPort, Integer httpPort, String propertyFilePath) throws RuntimeConfigurationException {
    if (jmxPort == null) {
      throw new RuntimeConfigurationError(TcServerBundle.message("validation.noJmxPort", propertyFilePath));
    }

    if (httpPort == null) {
      throw new RuntimeConfigurationError(TcServerBundle.message("validation.noHttpPort", propertyFilePath));
    }
  }
}
