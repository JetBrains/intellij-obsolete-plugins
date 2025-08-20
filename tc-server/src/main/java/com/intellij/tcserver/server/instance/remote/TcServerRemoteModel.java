package com.intellij.tcserver.server.instance.remote;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.transport.TransportHost;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.javaee.transport.TransportTarget;
import com.intellij.javaee.transport.local.LocalTransportHost;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.tcserver.server.instance.TcServerModelBase;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;

public class TcServerRemoteModel extends TcServerModelBase {

  private String myStagingRemotePath;

  private String myTransportHostId;

  private TransportTarget myTransportStagingTarget;

  private int myJmxPort;

  public String getStagingRemotePath() {
    return myStagingRemotePath;
  }

  public void setStagingRemotePath(String stagingRemotePath) {
    myStagingRemotePath = stagingRemotePath;
  }

  public String getTransportHostId() {
    return myTransportHostId;
  }

  public void setTransportHostId(String transportHostId) {
    myTransportHostId = transportHostId;
  }

  public TransportTarget getTransportStagingTarget() {
    return myTransportStagingTarget;
  }

  public void setTransportStagingTarget(TransportTarget transportStagingTarget) {
    myTransportStagingTarget = transportStagingTarget;
  }

  @Nullable
  private TransportHost getHost() {
    return TransportManager.getInstance().findHost(myTransportHostId, getProject());
  }

  public int getJmxPort() {
    return myJmxPort;
  }

  public void setJmxPort(int jmxPort) {
    myJmxPort = jmxPort;
  }

  @Override
  public String prepareDeployment(String sourcePath) throws RuntimeConfigurationException {
    validate(this);

    File sourceFile = new File(FileUtil.toSystemDependentName(sourcePath));
    TransportHost host = getHost();
    if (host == null) {
      throw new RuntimeConfigurationException(TcServerBundle.message("remoteModel.hostNotFound"));
    }

    if(host instanceof LocalTransportHost){
       return sourcePath;
    }

    if (!host.findOrCreateHostTarget(myTransportStagingTarget)
      .transfer(getProject(), Collections.singletonList(LocalFileSystem.getInstance().refreshAndFindFileByPath(sourcePath)))) {
      throw new RuntimeConfigurationException(TcServerBundle.message("remoteModel.transferInsuccessful", sourcePath));
    }
    String lastPathPart = sourceFile.getName();
    @SuppressWarnings("UnnecessaryLocalVariable") String result =
      FileUtil.toSystemIndependentName(myStagingRemotePath + "/" + lastPathPart);
    return result;
  }

  public static void validate(TcServerRemoteModel model) throws RuntimeConfigurationException {
    if (model.getTransportHostId() == null) {
      throw new RuntimeConfigurationError(TcServerBundle.message("remoteModel.remoteStagingNotSpecified"));
    }

    if (StringUtil.isEmpty(model.myStagingRemotePath)) {
      throw new RuntimeConfigurationError(TcServerBundle.message("remoteModel.remotePathNotSpecified"));
    }
  }

  @Override
  protected void read(@NotNull Element element, boolean isPersistent) throws InvalidDataException {
    final TcServerModelRemoteSettings settings = new TcServerModelRemoteSettings();

    XmlSerializer.deserializeInto(settings, element);

    readFromSettingsBase(settings, isPersistent);

    myStagingRemotePath = settings.getStagingRemotePath();
    myTransportHostId = settings.getTransportHostId();
    myTransportStagingTarget = settings.getTransportStagingTarget();
    myJmxPort = settings.getJmxPort();
  }

  @Override
  protected void write(@NotNull Element element, boolean isPersistent) throws WriteExternalException {
    final TcServerModelRemoteSettings settings = new TcServerModelRemoteSettings();

    writeToSettingsBase(settings, isPersistent);

    settings.setStagingRemotePath(myStagingRemotePath);
    settings.setTransportHostId(myTransportHostId);
    settings.setTransportStagingTarget(myTransportStagingTarget);
    settings.setJmxPort(myJmxPort);

    XmlSerializer.serializeInto(settings, element, new SkipDefaultValuesSerializationFilters());
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new TcRemoteServerRunConfigutationEditor();
  }

  @Override
  public boolean isApplicationServerNeeded() {
    return false;
  }

  public static class TcServerModelRemoteSettings extends TcServerModelSettingsBase {

    @Tag("staging-path")
    private String myStagingRemotePath;

    @Tag("host-id")
    private String myTransportHostId;

    @Tag("transport-staging-target")
    private TransportTarget myTransportStagingTarget;

    @Tag("jmx-port")
    private int myJmxPort = 6969;

    public String getTransportHostId() {
      return myTransportHostId;
    }

    public void setTransportHostId(String transportHostId) {
      myTransportHostId = transportHostId;
    }

    public String getStagingRemotePath() {
      return myStagingRemotePath;
    }

    public void setStagingRemotePath(String stagingRemotePath) {
      myStagingRemotePath = stagingRemotePath;
    }

    public TransportTarget getTransportStagingTarget() {
      return myTransportStagingTarget;
    }

    public void setTransportStagingTarget(TransportTarget transportStagingTarget) {
      myTransportStagingTarget = transportStagingTarget;
    }

    public int getJmxPort() {
      return myJmxPort;
    }

    public void setJmxPort(int jmxPort) {
      myJmxPort = jmxPort;
    }
  }
}
