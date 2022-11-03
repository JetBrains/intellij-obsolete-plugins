package com.intellij.dmserver.run;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.impl.DMServerConfigSupport2Base;
import com.intellij.dmserver.integration.DMServerIntegration;
import com.intellij.dmserver.integration.DMServerRepositoryItem;
import com.intellij.dmserver.integration.DMServerRepositoryWatchedItem;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.URLUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class DMServerModel extends DMServerModelBase {
  private static final Logger LOG = Logger.getInstance(DMServerModel.class);

  private String myTargetRepositoryFolder;

  @Override
  public boolean addToRepository(final List<VirtualFile> filesToDeploy) {
    final VirtualFile repositoryFolder = getRepositoryFolder();
    if (repositoryFolder == null) {
      return false;
    }
    try {
      WriteAction.run(()-> doAddToRepository(filesToDeploy, repositoryFolder));
      return true;
    }
    catch (IOException e) {
      LOG.error(e);
      return false;
    }
  }

  @Nullable
  private VirtualFile getRepositoryFolder() {
    Pair<DMServerRepositoryWatchedItem, RepositoryPattern> repositoryTarget = getRepositoryTarget();
    return repositoryTarget == null ? null : repositoryTarget.getSecond().findBaseDir();
  }

  @Nullable
  private Pair<DMServerRepositoryWatchedItem, RepositoryPattern> getRepositoryTarget() {
    if (myTargetRepositoryFolder == null) {
      return null;
    }

    DMServerInstallation serverInstallation = getServerInstallation();
    if (serverInstallation == null) {
      return null;
    }

    for (RepositoryPattern repositoryPattern : serverInstallation.collectRepositoryPatterns()) {
      DMServerRepositoryItem repositoryItem = repositoryPattern.getSource();
      if ((repositoryItem instanceof DMServerRepositoryWatchedItem) && myTargetRepositoryFolder.equals(repositoryItem.getPath())) {
        return Pair.create((DMServerRepositoryWatchedItem)repositoryItem, repositoryPattern);
      }
    }
    return null;
  }

  private void doAddToRepository(List<VirtualFile> filesToDeploy, VirtualFile repositoryFolder) throws IOException {
    for (VirtualFile fileToDeploy : filesToDeploy) {
      fileToDeploy.copy(this, repositoryFolder, fileToDeploy.getName());
    }
  }

  @Override
  public boolean removeFromRepository(final List<VirtualFile> filesToUndeploy) {
    final VirtualFile repositoryFolder = getRepositoryFolder();
    if (repositoryFolder == null) {
      return false;
    }
    try {
      WriteAction.run(()-> doRemoveFromRepository(filesToUndeploy, repositoryFolder));
      return true;
    }
    catch (IOException e) {
      LOG.error(e);
      return false;
    }
  }

  private void doRemoveFromRepository(List<VirtualFile> filesToUndeploy, VirtualFile repositoryFolder) throws IOException {
    repositoryFolder.refresh(false, false);
    for (VirtualFile fileToUndeploy : filesToUndeploy) {
      VirtualFile repositoryFileToUndeploy = repositoryFolder.findChild(fileToUndeploy.getName());
      if (repositoryFileToUndeploy != null) {
        repositoryFileToUndeploy.delete(this);
      }
    }
  }

  @Nullable
  @Override
  public String getRepositoryName() {
    Pair<DMServerRepositoryWatchedItem, RepositoryPattern> repositoryTarget = getRepositoryTarget();
    return repositoryTarget == null ? null : repositoryTarget.getFirst().getName();
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new DMLocalRunConfigurationEditor(getCommonModel().getProject(), this);
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    return Collections.singletonList(Pair.create(getCommonModel().getHost(), getLocalPort()));
  }

  @Override
  protected void read(@NotNull Element element, boolean isPersistent) throws InvalidDataException {
    final DMServerModelSettings settings = new DMServerModelSettings();

    XmlSerializer.deserializeInto(settings, element);

    readFromSettingsBase(settings, isPersistent);

    myTargetRepositoryFolder = settings.getTargetRepositoryFolder();
  }

  @Override
  protected void write(@NotNull Element element, boolean isPersistent) throws WriteExternalException {
    final DMServerModelSettings settings = new DMServerModelSettings();

    writeToSettingsBase(settings, isPersistent);

    settings.setTargetRepositoryFolder(myTargetRepositoryFolder);

    XmlSerializer.serializeInto(settings, element, new SkipDefaultValuesSerializationFilters());
  }

  @Nullable
  private DMServerInstallation getServerInstallation() {
    return DMServerIntegration.getInstance().getServerInstallation(getCommonModel());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    DMServerInstallation serverInstallation = getServerInstallation();
    if (serverInstallation != null && serverInstallation.getConfigSupport() instanceof DMServerConfigSupport2Base &&
        myTargetRepositoryFolder == null) {
      throw new RuntimeConfigurationError(DmServerBundle.message("DMServerModel.error.target.repository.folder.not.configured"));
    }
  }

  @Override
  public URL computeServerAccessibleStagingURL(String deploymentSourcePath) throws MalformedURLException {
    return new URL(URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + new File(deploymentSourcePath).toURI().getRawPath());
  }

  @Override
  public boolean prepareDeploy(VirtualFile fileToDeploy) {
    // nothing
    return true;
  }

  public String getTargetRepositoryFolder() {
    return myTargetRepositoryFolder;
  }

  public void setTargetRepositoryFolder(String targetRepositoryFolder) {
    myTargetRepositoryFolder = targetRepositoryFolder;
  }

  public static class DMServerModelSettings extends DMServerModelSettingsBase {

    @Tag("target-repository")
    private String myTargetRepositoryFolder;

    public String getTargetRepositoryFolder() {
      return myTargetRepositoryFolder;
    }

    public void setTargetRepositoryFolder(String targetRepositoryFolder) {
      myTargetRepositoryFolder = targetRepositoryFolder;
    }
  }
}
