package com.intellij.tcserver.server.instance;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerUrlMapping;
import com.intellij.javaee.appServers.run.configuration.ServerModelBase;
import com.intellij.javaee.appServers.run.execution.DefaultOutputProcessor;
import com.intellij.javaee.appServers.run.execution.OutputProcessor;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.deployment.TcServerDeploymentProvider;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.server.instance.remote.TcServerRemoteModel;
import com.intellij.tcserver.server.integration.TcServerData;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class TcServerModelBase extends ServerModelBase {
  public static final String TC_INSTANCE_SERVICE_MODE = "service";
  public static final String TC_INSTANCE_BATCH_MODE = "batch";
  private static final String DEFAULT_JMX_PASSWORD = "springsource";

  private CommonModel myCommonModel;
  private boolean myIsJmxAuthenticationEnabled;
  private String myJmxLogin;
  private String myJmxPassword;
  private String myCredentialAlias;
  private String myInstanceMode;
  private boolean myIsTemplate;

  @Override
  public J2EEServerInstance createServerInstance() {
    return new TcServerInstance(myCommonModel);
  }

  @Override
  public DeploymentProvider getDeploymentProvider() {
    return new TcServerDeploymentProvider();
  }

  @Override
  @NotNull
  public String getDefaultUrlForBrowser() {
    ApplicationServerUrlMapping urlMapping = (ApplicationServerUrlMapping)myCommonModel.getIntegration().getDeployedFileUrlProvider();
    return urlMapping.getDefaultUrlForServerConfig(myCommonModel);
  }

  @Nullable
  private Integer getPort() {
    if (myCommonModel == null || myCommonModel.getApplicationServer() == null) {
      return null;
    }
    TcServerData data = (TcServerData)myCommonModel.getApplicationServer().getPersistentData();
    return data.getHttpPort();
  }

  @Override
  public OutputProcessor createOutputProcessor(ProcessHandler processHandler, J2EEServerInstance serverInstance) {
    return new DefaultOutputProcessor(processHandler);
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    Integer port = getPort();
    if (port != null) {
      return Collections.singletonList(Pair.create(myCommonModel.getHost(), port));
    }
    else {
      return Collections.emptyList();
    }
  }

  @Override
  public void checkConfiguration() {
    //configuration is always valid
  }

  @Override
  public int getDefaultPort() {
    return 8080;
  }

  @Override
  public void setCommonModel(CommonModel commonModel) {
    myCommonModel = commonModel;
  }

  @Override
  public CommonModel getCommonModel() {
    return myCommonModel;
  }

  @Override
  public int getLocalPort() {
    Integer port = getPort();
    return port != null ? port : getDefaultPort();
  }

  public boolean isJmxAuthenticationEnabled() {
    return myIsJmxAuthenticationEnabled;
  }

  public String getLogin() {
    return myJmxLogin;
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
    return myJmxPassword;
  }

  @Override
  public void setPassword(@NotNull String jmxPassword) {
    myJmxPassword = jmxPassword;
  }

  @Override
  protected boolean isDefaultPasswords() {
    return DEFAULT_JMX_PASSWORD.equals(myJmxPassword);
  }

  @Nullable
  @Override
  @NlsSafe
  protected String getCredentialAlias() {
    return myCredentialAlias;
  }

  @Override
  protected void setCredentialAlias(@Nullable String credentialAlias) {
    myCredentialAlias = credentialAlias;
  }

  public final Project getProject() {
    return getCommonModel().getProject();
  }

  @NlsSafe
  public String getInstanceMode() {
    return myInstanceMode;
  }

  protected void readFromSettingsBase(@NotNull TcServerModelSettingsBase settings, boolean isPersistent) {
    myIsJmxAuthenticationEnabled = settings.getMyIsJmxAuthenticationEnabled();
    myJmxLogin = settings.getLogin();
    myInstanceMode = settings.getInstanceMode();

    if (!isPersistent) {
      myIsTemplate = settings.isTemplate();
    }

    if (!myIsTemplate) {
      myJmxPassword = settings.getPassword();
      myCredentialAlias = settings.getCredentialAlias();
    }
  }

  protected void writeToSettingsBase(@NotNull TcServerModelSettingsBase settings, boolean isPersistent) {
    settings.setMyIsJmxAuthenticationEnabled(myIsJmxAuthenticationEnabled);
    settings.setLogin(myJmxLogin);
    settings.setInstanceMode(myInstanceMode);

    if (!myIsTemplate) {
      if (!isPersistent) {
        settings.setPassword(myJmxPassword);
      }

      settings.setCredentialAlias(myCredentialAlias);
    }

    if (!isPersistent) {
      settings.setTemplate(myIsTemplate);
    }
  }

  public void setIsJmxAuthenticationEnabled(boolean isJmxAuthenticationEnabled) {
    myIsJmxAuthenticationEnabled = isJmxAuthenticationEnabled;
  }

  public void setLogin(String jmxLogin) {
    myJmxLogin = jmxLogin;
  }

  public void setInstanceMode(String instanceMode) {
    myInstanceMode = instanceMode;
  }

  public abstract String prepareDeployment(String sourcePath) throws RuntimeConfigurationException;

  public abstract static class TcServerModelSettingsBase {
    @Tag("isAuthEnabled")
    private boolean myIsJmxAuthenticationEnabled = true;
    @NonNls @Tag("jmxLogin")
    private String myJmxLogin = "admin";
    @NonNls @Tag("jmxPassword")
    private String myJmxPassword = DEFAULT_JMX_PASSWORD;
    @NonNls @Tag("credentialAlias")
    private String myCredentialAlias;
    @NonNls @Tag("instanceMode")
    private String myInstanceMode = TC_INSTANCE_SERVICE_MODE;
    @NonNls @Tag("template")
    private boolean myIsTemplate;

    public boolean getMyIsJmxAuthenticationEnabled() {
      return myIsJmxAuthenticationEnabled;
    }

    public void setMyIsJmxAuthenticationEnabled(boolean isJmxAuthenticationEnabled) {
      myIsJmxAuthenticationEnabled = isJmxAuthenticationEnabled;
    }

    public String getLogin() {
      return myJmxLogin;
    }

    public void setLogin(String login) {
      myJmxLogin = login;
    }

    public String getPassword() {
      return myJmxPassword;
    }

    public void setPassword(String password) {
      myJmxPassword = password;
    }

    public String getCredentialAlias() {
      return myCredentialAlias;
    }

    public void setCredentialAlias(String credentialAlias) {
      myCredentialAlias = credentialAlias;
    }

    public String getInstanceMode() {
      return myInstanceMode;
    }

    public void setInstanceMode(String instanceMode) {
      myInstanceMode = instanceMode;
    }

    public boolean isTemplate() {
      return myIsTemplate;
    }

    public void setTemplate(boolean isTemplate) {
      myIsTemplate = isTemplate;
    }
  }

  public static void verifyCommonModel(CommonModel commonModel) throws RuntimeConfigurationException {
    if (commonModel.getServerModel() instanceof TcServerLocalModel) {
      if (commonModel.getApplicationServer() == null || commonModel.getApplicationServer().getPersistentData() == null) {
        throw new RuntimeConfigurationError(TcServerBundle.message("validation.noApplicationServer"));
      }

      TcServerData serverData = (TcServerData)commonModel.getApplicationServer().getPersistentData();
      TcServerData.validateTcServerData(serverData.getSdkPath(), serverData.getServerName(), serverData.getAvailableServers());
      TcServerData.validatePorts(serverData.getJmxPort(), serverData.getHttpPort(),
                                 TcServerUtil.getCatalinaPropertiesPath(serverData.getSdkPath(), serverData.getServerName()));
    }


    if (commonModel.getServerModel() instanceof TcServerRemoteModel && !commonModel.getDeployedArtifacts().isEmpty()) {
      TcServerRemoteModel.validate((TcServerRemoteModel)commonModel.getServerModel());
    }
  }

  public List<String> getInstanceModes() {
    return Arrays.asList(TC_INSTANCE_SERVICE_MODE, TC_INSTANCE_BATCH_MODE);
  }

  public boolean isBatchMode() {
    return SystemInfo.isWindows && StringUtil.equals(TC_INSTANCE_BATCH_MODE, getInstanceMode());
  }
}
