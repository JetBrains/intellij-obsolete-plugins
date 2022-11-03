package com.intellij.dmserver.run;

import com.intellij.dmserver.DMConstants;
import com.intellij.dmserver.artifacts.*;
import com.intellij.dmserver.deploy.DMServerDeploymentProvider;
import com.intellij.dmserver.facet.DMCompositeFacet;
import com.intellij.dmserver.facet.NestedUnitIdentity;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerUrlMapping;
import com.intellij.javaee.appServers.run.configuration.ServerModelBase;
import com.intellij.javaee.appServers.run.execution.DefaultOutputProcessor;
import com.intellij.javaee.appServers.run.execution.OutputProcessor;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class DMServerModelBase extends ServerModelBase {
  private CommonModel myCommonModel;
  private int myPort = DMConstants.DEFAULT_PORT;

  private String myMBeanServerUserName = DMConstants.DEFAULT_MBEAN_SERVER_USERNAME;
  private int myMBeanServerPort = DMConstants.DEFAULT_MBEAN_SERVER_PORT;
  private String myMBeanServerPassword = DMConstants.DEFAULT_MBEAN_SERVER_PASSWORD;
  private String myCredentialAlias;

  private boolean myIsTemplate;

  @Override
  public final void setCommonModel(CommonModel commonModel) {
    myCommonModel = commonModel;
  }

  public abstract boolean prepareDeploy(VirtualFile fileToDeploy);

  public abstract URL computeServerAccessibleStagingURL(String deploymentSourcePath) throws MalformedURLException;

  public abstract boolean addToRepository(List<VirtualFile> filesToDeploy);

  public abstract boolean removeFromRepository(List<VirtualFile> filesToUndeploy);

  @Nullable
  public abstract String getRepositoryName();

  @Override
  public final CommonModel getCommonModel() {
    return myCommonModel;
  }

  public final Project getProject() {
    return getCommonModel().getProject();
  }

  @Override
  public final J2EEServerInstance createServerInstance() throws ExecutionException {
    DMServerIntegrationData integrationData = (DMServerIntegrationData)getCommonModel().getApplicationServer().getPersistentData();
    DMServerInstallation installation = integrationData.getInstallation();
    if (installation == null) {
      throw new ExecutionException(DmServerBundle.message("DMServerModelBase.error.can.not.locate.dmserver.home"));
    }
    if (!installation.isValid()) {
      throw new ExecutionException(DmServerBundle.message("DMServerModelBase.error.can.not.locate.startup.shutdown.script"));
    }
    return new DMServerInstance(myCommonModel, installation.getServerVersion());
  }

  @Override
  public final DeploymentProvider getDeploymentProvider() {
    return new DMServerDeploymentProvider();
  }

  @Override
  public int getDefaultPort() {
    return DMConstants.DEFAULT_PORT;
  }

  @Override
  @NotNull
  @NonNls
  public String getDefaultUrlForBrowser() {
    ApplicationServerUrlMapping urlMapping = (ApplicationServerUrlMapping)myCommonModel.getIntegration().getDeployedFileUrlProvider();
    String webContext = searchWebContext(myCommonModel.getDeployedArtifacts());
    return urlMapping.getDefaultUrlForServerConfig(myCommonModel) + StringUtil.notNullize(webContext);
  }

  public String searchWebContext(List<Artifact> artifacts) {
    String context = null;
    for (Artifact artifact : artifacts) {
      Module module = WithModuleArtifactUtil.findModuleFor(myCommonModel.getProject(), artifact);
      ArtifactType artifactType = artifact.getArtifactType();
      if (artifactType == DMBundleArtifactType.getInstance()) {
        if (module == null) {
          return null;
        }
        ManifestManager.FileWrapper manifestFile = ManifestManager.getBundleInstance().findManifest(module);
        if (manifestFile == null) {
          return null;
        }
        context = ManifestUtils.getInstance().getHeaderValue(manifestFile.getFile(), ManifestUtils.WEB_CONTEXT_PATH_HEADER);
      }
      if (artifactType == DMPlanArtifactType.getInstance() ||
          artifactType == DMParArtifactType.getInstance()) {
        DMCompositeFacet facet = DMCompositeFacet.getInstance(module);
        if (facet == null) {
          return null;
        }
        List<NestedUnitIdentity> nestedUnitIdentities = (List<NestedUnitIdentity>)facet.getConfigurationImpl().getNestedBundles();
        for (NestedUnitIdentity nest : nestedUnitIdentities) {
          List<Artifact> nestArtifacts = WithModuleArtifactUtil.findWithModuleArtifactsFor(nest.getModule());
          context = searchWebContext(nestArtifacts);
        }
        if (context == null) {
          return null;
        }
      }
    }
    return context;
  }

  @Override
  public OutputProcessor createOutputProcessor(ProcessHandler processHandler, J2EEServerInstance serverInstance) {
    return new DefaultOutputProcessor(processHandler);
  }

  @Override
  public int getLocalPort() {
    return myPort;
  }

  public void setPort(int port) {
    myPort = port;
  }

  @Override
  public boolean isTemplate() {
    return myIsTemplate;
  }

  @Override
  public void setTemplate(boolean isTemplate) {
    myIsTemplate = isTemplate;
  }

  @Nullable
  @Override
  public String getPassword() {
    return getMBeanServerPassword();
  }

  @Override
  public void setPassword(@NotNull String password) {
    setMBeanServerPassword(password);
  }

  @Override
  protected boolean isDefaultPasswords() {
    return DMConstants.DEFAULT_MBEAN_SERVER_PASSWORD.equals(getPassword());
  }

  @Nullable
  @Override
  protected String getCredentialAlias() {
    return myCredentialAlias;
  }

  @Override
  protected void setCredentialAlias(@Nullable String credentialAlias) {
    myCredentialAlias = credentialAlias;
  }

  public String getMBeanServerUserName() {
    return myMBeanServerUserName;
  }

  public void setMBeanServerUserName(String beanServerUserName) {
    myMBeanServerUserName = beanServerUserName;
  }

  public int getMBeanServerPort() {
    return myMBeanServerPort;
  }

  public void setMBeanServerPort(int beanServerPort) {
    myMBeanServerPort = beanServerPort;
  }

  public String getMBeanServerPassword() {
    return myMBeanServerPassword;
  }

  public void setMBeanServerPassword(String beanServerPassword) {
    myMBeanServerPassword = beanServerPassword;
  }

  protected void readFromSettingsBase(@NotNull DMServerModelSettingsBase settings, boolean isPersistent) {
    myPort = settings.getPort();
    myMBeanServerUserName = settings.getMBeanUserName();
    myMBeanServerPort = settings.getMBeanPort();

    if (!isPersistent) {
      myIsTemplate = settings.isTemplate();
    }

    if (!myIsTemplate) {
      myMBeanServerPassword = settings.getMBeanPassword();
      myCredentialAlias = settings.getCredentialAlias();
    }
  }

  protected void writeToSettingsBase(@NotNull DMServerModelSettingsBase settings, boolean isPersistent) {
    settings.setPort(myPort);
    settings.setMBeanUserName(myMBeanServerUserName);
    settings.setMBeanPort(myMBeanServerPort);

    if (!myIsTemplate) {
      if (!isPersistent) {
        settings.setMBeanPassword(myMBeanServerPassword);
      }

      settings.setCredentialAlias(myCredentialAlias);
    }

    if (!isPersistent) {
      settings.setTemplate(myIsTemplate);
    }
  }

  public abstract static class DMServerModelSettingsBase {
    @Tag("port")
    private int myPort = DMConstants.DEFAULT_PORT;
    @Tag("mbean-port")
    private int myMBeanPort;
    @Tag("mbean-user")
    private String myMBeanUserName;
    @Tag("mbean-password")
    private String myMBeanPassword;
    @Tag("credentialAlias")
    private String myCredentialAlias;
    @Tag("template")
    private boolean myIsTemplate;

    public int getPort() {
      return myPort;
    }

    public void setPort(int port) {
      myPort = port;
    }

    public int getMBeanPort() {
      return myMBeanPort;
    }

    public void setMBeanPort(int mBeanPort) {
      myMBeanPort = mBeanPort;
    }

    public String getMBeanUserName() {
      return myMBeanUserName;
    }

    public void setMBeanUserName(String mBeanUserName) {
      myMBeanUserName = mBeanUserName;
    }

    public String getMBeanPassword() {
      return myMBeanPassword;
    }

    public void setMBeanPassword(String mBeanPassword) {
      myMBeanPassword = mBeanPassword;
    }

    public String getCredentialAlias() {
      return myCredentialAlias;
    }

    public void setCredentialAlias(String credentialAlias) {
      myCredentialAlias = credentialAlias;
    }

    public boolean isTemplate() {
      return myIsTemplate;
    }

    public void setTemplate(boolean isTemplate) {
      myIsTemplate = isTemplate;
    }
  }
}
