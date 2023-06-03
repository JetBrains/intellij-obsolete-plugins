package org.intellij.j2ee.web.resin;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.transport.TransportHost;
import com.intellij.javaee.transport.TransportHostTarget;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.javaee.transport.TransportTarget;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Tag;
import org.intellij.j2ee.web.resin.ui.RemoteRunConfigurationEditor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ResinRemoteModel extends ResinModelBase<ResinRemoteModel.ResinRemoteModelData> {

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new RemoteRunConfigurationEditor(getProject());
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    return Collections.emptyList();
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (!hasJmxStrategy() && getCommonModel().getDeploymentModels().size() > 0) {
      throw new RuntimeConfigurationError("Remote deployment is not supported for Resin 2.x");
    }
    super.checkConfiguration();
  }

  public String getTransportHostId() {
    return getData().getTransportHostId();
  }

  public void setTransportHostId(String transportHostId) {
    getData().setTransportHostId(transportHostId);
  }

  public TransportTarget getTransportTargetWebApps() {
    return getData().getTransportTargetWebApps();
  }

  public void setTransportTargetWebApps(TransportTarget transportTargetWebApps) {
    getData().setTransportTargetWebApps(transportTargetWebApps);
  }

  @Override
  protected ResinRemoteModelData createResinModelData() {
    return new ResinRemoteModelData();
  }

  @Nullable
  private TransportHost getHost() {
    return TransportManager.getInstance().findHost(getTransportHostId(), getProject());
  }

  @Nullable
  private TransportHostTarget getTransportHostTarget() {
    TransportHost host = getHost();
    return host == null ? null : host.findOrCreateHostTarget(getTransportTargetWebApps());
  }

  @Override
  public boolean transferFile(File webAppFile) {
    TransportHostTarget target = getTransportHostTarget();
    return target != null && target.transfer(getProject(),
      Collections.singletonList(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(webAppFile)));
  }

  @Override
  public boolean deleteFile(File webAppFile) {
    TransportHostTarget target = getTransportHostTarget();
    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(webAppFile);
    if (vFile == null) {
      return true;
    }
    return target != null && target.delete(getProject(), Collections.singletonList(vFile));
  }

  @Nullable
  @Override
  public SettingsEditor<DeploymentModel> createAdditionalDeploymentSettingsEditor(CommonModel commonModel, DeploymentSource source) {
    return null;
  }

  public static class ResinRemoteModelData extends ResinModelDataBase {

    @Tag("host-id")
    private String myTransportHostId;

    @Tag("transport-target-webapps")
    private TransportTarget myTransportTargetWebApps;

    public String getTransportHostId() {
      return myTransportHostId;
    }

    public void setTransportHostId(String transportHostId) {
      myTransportHostId = transportHostId;
    }

    public TransportTarget getTransportTargetWebApps() {
      return myTransportTargetWebApps;
    }

    public void setTransportTargetWebApps(TransportTarget transportTargetWebApps) {
      myTransportTargetWebApps = transportTargetWebApps;
    }
  }
}
