package com.intellij.dmserver.run.remote;

import com.intellij.dmserver.run.DMServerModelBase;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.transport.TransportHost;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.javaee.transport.TransportTarget;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class DMServerRemoteModel extends DMServerModelBase {

  private String myJmxMappingUrl;

  private String myTransportHostId;

  private TransportTarget myTransportTargetRepository;
  private TransportTarget myTransportTargetJmx;
  private String myRepositoryName;

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    return Collections.emptyList();
  }

  @Override
  protected void read(@NotNull Element element, boolean isPersistent) throws InvalidDataException {
    final DMServerRemoteModelSettings settings = new DMServerRemoteModelSettings();

    XmlSerializer.deserializeInto(settings, element);

    readFromSettingsBase(settings, isPersistent);

    myJmxMappingUrl = settings.getJmxMappingUrl();
    myRepositoryName = settings.getRepositoryName();

    myTransportHostId = settings.getTransportHostId();
    myTransportTargetRepository = settings.getTransportTargetRepository();
    myTransportTargetJmx = settings.getTransportTargetJmx();
  }

  @Override
  protected void write(@NotNull Element element, boolean isPersistent) throws WriteExternalException {
    final DMServerRemoteModelSettings settings = new DMServerRemoteModelSettings();

    writeToSettingsBase(settings, isPersistent);

    settings.setJmxMappingUrl(myJmxMappingUrl);
    settings.setRepositoryName(myRepositoryName);

    settings.setTransportHostId(myTransportHostId);
    settings.setTransportTargetRepository(myTransportTargetRepository);
    settings.setTransportTargetJmx(myTransportTargetJmx);

    XmlSerializer.serializeInto(settings, element, new SkipDefaultValuesSerializationFilters());
  }

  @Override
  public URL computeServerAccessibleStagingURL(String deploymentSourcePath) throws MalformedURLException {
    if (myJmxMappingUrl == null) {
      throw new MalformedURLException("Wow, staging folder is not mapped, configuration should not pass validation");
    }
    return createJmxMappingUrl();
  }

  @Nullable
  private TransportHost getHost() {
    return TransportManager.getInstance().findHost(myTransportHostId, getProject());
  }

  @Override
  public boolean addToRepository(List<VirtualFile> filesToDeploy) {
    TransportHost host = getHost();
    return host != null && host.findOrCreateHostTarget(myTransportTargetRepository).transfer(getProject(), filesToDeploy);
  }

  @Override
  public boolean removeFromRepository(final List<VirtualFile> filesToUndeploy) {
    TransportHost host = getHost();
    return host != null && host.findOrCreateHostTarget(myTransportTargetRepository).delete(getProject(), filesToUndeploy);
  }

  @Override
  public String getRepositoryName() {
    return myRepositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    myRepositoryName = repositoryName;
  }

  @Override
  public boolean prepareDeploy(VirtualFile fileToDeploy) {
    TransportHost host = getHost();
    return host != null &&
           host.findOrCreateHostTarget(myTransportTargetJmx).transfer(getProject(), Collections.singletonList(fileToDeploy));
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    // TODO: may need additional checks
    validateMappingURL();
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new DMRemoteRunConfigurationEditor(getProject());
  }

  private void validateMappingURL() throws RuntimeConfigurationException {
    if (StringUtil.isEmptyOrSpaces(myJmxMappingUrl)) {
      throw new RuntimeConfigurationError(DmServerBundle.message("DMServerRemoteModel.error.mapping.url.not.configured"));
    }
    try {
      createJmxMappingUrl();
    }
    catch (MalformedURLException ignored) {
      throw new RuntimeConfigurationError(DmServerBundle.message("DMServerRemoteModel.error.mapping.url.not.valid", myJmxMappingUrl));
    }
  }

  private URL createJmxMappingUrl() throws MalformedURLException {
    String fixCommonProblems = myJmxMappingUrl.replaceAll(" ", "%20");
    if (!fixCommonProblems.endsWith("/")) {
      fixCommonProblems += "/";
    }
    return new URL(fixCommonProblems);
  }

  public String getJmxMappingUrl() {
    return myJmxMappingUrl;
  }

  public void setJmxMappingUrl(String jmxMappingUrl) {
    myJmxMappingUrl = jmxMappingUrl;
  }

  public String getTransportHostId() {
    return myTransportHostId;
  }

  public void setTransportHostId(String transportHostId) {
    myTransportHostId = transportHostId;
  }

  public TransportTarget getTransportTargetJmx() {
    return myTransportTargetJmx;
  }

  public void setTransportTargetJmx(TransportTarget transportTargetJmx) {
    myTransportTargetJmx = transportTargetJmx;
  }

  public TransportTarget getTransportTargetRepository() {
    return myTransportTargetRepository;
  }

  public void setTransportTargetRepository(TransportTarget transportTargetRepository) {
    myTransportTargetRepository = transportTargetRepository;
  }

  public static class DMServerRemoteModelSettings extends DMServerModelSettingsBase {

    @Tag("staging-url")
    private String myJmxMappingUrl;

    @Tag("host-id")
    private String myTransportHostId;

    @Tag("transport-target-repository")
    private TransportTarget myTransportTargetRepository;

    @Tag("transport-target-staging")
    private TransportTarget myTransportTargetJmx;

    @Tag("repository-name")
    private String myRepositoryName;

    public String getJmxMappingUrl() {
      return myJmxMappingUrl;
    }

    public void setJmxMappingUrl(String stagingFolderURL) {
      myJmxMappingUrl = stagingFolderURL;
    }

    public String getTransportHostId() {
      return myTransportHostId;
    }

    public void setTransportHostId(String transportHostId) {
      myTransportHostId = transportHostId;
    }

    public TransportTarget getTransportTargetRepository() {
      return myTransportTargetRepository;
    }

    public void setTransportTargetRepository(TransportTarget transportTargetRepository) {
      myTransportTargetRepository = transportTargetRepository;
    }

    public TransportTarget getTransportTargetJmx() {
      return myTransportTargetJmx;
    }

    public void setTransportTargetJmx(TransportTarget transportTargetJmx) {
      myTransportTargetJmx = transportTargetJmx;
    }

    public String getRepositoryName() {
      return myRepositoryName;
    }

    public void setRepositoryName(String repositoryName) {
      myRepositoryName = repositoryName;
    }
  }
}
