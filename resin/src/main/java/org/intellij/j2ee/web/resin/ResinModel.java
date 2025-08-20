package org.intellij.j2ee.web.resin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.directoryManager.SystemBaseDirectoryManager;
import com.intellij.javaee.jmxremote.JmxRemoteAware;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.j2ee.web.resin.resin.ResinConfiguration;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.intellij.j2ee.web.resin.ui.DeploymentSettingsEditor;
import org.intellij.j2ee.web.resin.ui.RunConfigurationEditor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ResinModel extends ResinModelBase<ResinModel.ResinLocalModelData> implements JmxRemoteAware {

  private static final Logger LOG = Logger.getInstance(ResinModel.class);

  @NonNls
  public static final String DEPLOY_MODE_AUTO = "automatic";
  @NonNls
  public static final String DEPLOY_MODE_LAZY = "lazy";
  @NonNls
  public static final String DEPLOY_MODE_MANUAL = "manual";

  private ResinConfiguration myConfiguration;

  private String myJmxUsername;
  private String myJmxPassword;

  private File myAccessFile;
  private File myPasswordFile;

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new RunConfigurationEditor();
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    return Collections.singletonList(Pair.create(getCommonModel().getHost(), getLocalPort()));
  }

  public ResinConfiguration getOrCreateResinConfiguration(boolean forceCreation) throws ExecutionException {
    if (myConfiguration == null || forceCreation) {
      myConfiguration = new ResinConfiguration(this);
    }
    return myConfiguration;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    findConfFile();
    super.checkConfiguration();
  }

  public File findConfFile() throws RuntimeConfigurationException {
    File sourceConfig = null;

    ResinPersistentData persistentData = getHelper().getPersistentData();
    if (persistentData != null && !StringUtil.isEmpty(persistentData.RESIN_CONF)) {
      sourceConfig = new File(persistentData.RESIN_CONF);
    }

    String sourceConfigPath = getResinConf();
    if (!StringUtil.isEmpty(sourceConfigPath)) {
      sourceConfig = new File(FileUtil.toSystemDependentName(sourceConfigPath));
    }

    if (sourceConfig == null) {
      throw new RuntimeConfigurationException(ResinBundle.message("message.error.resin.conf.doesnt.chosen"));
    }
    if (!sourceConfig.exists()) {
      throw new RuntimeConfigurationException(ResinBundle.message("message.error.resin.conf.doesnt.exist", sourceConfig.getAbsolutePath()));
    }
    if (sourceConfig.isDirectory()) {
      throw new RuntimeConfigurationException(ResinBundle.message("message.error.resin.conf.directory", sourceConfig.getAbsolutePath()));
    }

    return sourceConfig;
  }

  public boolean isDebugConfiguration() {
    return getData().isDebugConfiguration();
  }

  public void setDebugConfiguration(boolean debugConfiguration) {
    getData().setDebugConfiguration(debugConfiguration);
  }

  public boolean isAutoBuildClassPath() {
    return getData().isAutoBuildClassPath();
  }

  public void setAutoBuildClassPath(boolean autoBuildClassPath) {
    getData().setAutoBuildClassPath(autoBuildClassPath);
  }

  public String getResinConf() {
    return getData().getResinConf();
  }

  public void setResinConf(String resinConf) {
    getData().setResinConf(resinConf);
  }

  public boolean isReadOnlyConfiguration() {
    return getData().isReadOnlyConfiguration();
  }

  public void setReadOnlyConfiguration(boolean readOnlyConfiguration) {
    getData().setReadOnlyConfiguration(readOnlyConfiguration);
  }

  public String getAdditionalParameters() {
    String additionalParameters = getData().getAdditionalParameters();
    return additionalParameters == null ? "" : additionalParameters.trim();
  }

  public void setAdditionalParameters(String additionalParameters) {
    getData().setAdditionalParameters(additionalParameters);
  }

  public String getDeployMode() {
    String deployMode = getData().getDeployMode();
    return deployMode == null ? DEPLOY_MODE_MANUAL : deployMode;
  }

  public void setDeployMode(String deployMode) {
    getData().setDeployMode(deployMode);
  }

  @Override
  protected ResinLocalModelData createResinModelData() {
    return new ResinLocalModelData();
  }

  private File getWebAppFileDestination(File webAppFile) {
    final ResinInstallation installation = getInstallation();
    LOG.assertTrue(installation != null);

    File homeDir = installation.getResinHome();

    final File webAppsDir = new File(homeDir, "webapps"); // TODO: take from config
    if (!webAppsDir.exists()) {
      LOG.error("Can't find webapps folder");
      return null;
    }

    return new File(webAppsDir, webAppFile.getName());
  }

  @Override
  public boolean transferFile(final File webAppFile) {
    File webAppFileDestination = getWebAppFileDestination(webAppFile);
    if (webAppFileDestination == null) {
      return false;
    }

    try {
      FileUtil.copyFileOrDir(webAppFile, webAppFileDestination);
    }
    catch (IOException e) {
      LOG.error(e);
      return false;
    }

    return true;
  }

  @Override
  public boolean deleteFile(File webAppFile) {
    File webAppFileDestination = getWebAppFileDestination(webAppFile);
    if (webAppFileDestination == null) {
      return false;
    }

    return FileUtil.delete(webAppFileDestination);
  }

  @Override
  public SettingsEditor<DeploymentModel> createAdditionalDeploymentSettingsEditor(CommonModel commonModel, DeploymentSource source) {
    return new DeploymentSettingsEditor(commonModel, source);
  }

  @Nullable
  @Override
  public String getJmxUsername() {
    return myJmxUsername;
  }

  @Override
  public void setJmxUsername(@Nullable String jmxUsername) {
    myJmxUsername = jmxUsername;
  }

  @Nullable
  @Override
  public String getJmxPassword() {
    return myJmxPassword;
  }

  @Override
  public void setJmxPassword(@Nullable String jmxPassword) {
    myJmxPassword = jmxPassword;
  }

  @NotNull
  @Override
  public SystemBaseDirectoryManager getSystemBaseDirectoryManager() {
    return ResinSystemBaseDirectoryManager.getInstance();
  }

  @Nullable
  @Override
  public String getBaseDirectoryName() {
    return getData().getBaseDirectoryName();
  }

  @Override
  public void setBaseDirectoryName(@Nullable String baseDirectoryName) {
    getData().setBaseDirectoryName(baseDirectoryName);
  }

  @Nullable
  public File getAccessFile() {
    return myAccessFile;
  }

  public void setAccessFile(@Nullable File accessFile) {
    myAccessFile = accessFile;
  }

  @Nullable
  public File getPasswordFile() {
    return myPasswordFile;
  }

  public void setPasswordFile(@Nullable File passwordFile) {
    myPasswordFile = passwordFile;
  }

  public static class ResinLocalModelData extends ResinModelDataBase {

    private String myResinConf = "";
    private boolean myDebugConfiguration = false;
    private boolean myAutoBuildClassPath = false;
    private boolean myReadOnlyConfiguration = false;
    private String myAdditionalParameters = "";

    private String myDeployMode = DEPLOY_MODE_AUTO;

    private String myBaseDirectoryName;

    public String getResinConf() {
      return myResinConf;
    }

    public void setResinConf(String resinConf) {
      myResinConf = resinConf;
    }

    public boolean isDebugConfiguration() {
      return myDebugConfiguration;
    }

    public void setDebugConfiguration(boolean debugConfiguration) {
      myDebugConfiguration = debugConfiguration;
    }

    public boolean isAutoBuildClassPath() {
      return myAutoBuildClassPath;
    }

    public void setAutoBuildClassPath(boolean autoBuildClassPath) {
      myAutoBuildClassPath = autoBuildClassPath;
    }

    public boolean isReadOnlyConfiguration() {
      return myReadOnlyConfiguration;
    }

    public void setReadOnlyConfiguration(boolean readOnlyConfiguration) {
      myReadOnlyConfiguration = readOnlyConfiguration;
    }

    public String getAdditionalParameters() {
      return myAdditionalParameters;
    }

    public void setAdditionalParameters(String additionalParameters) {
      myAdditionalParameters = additionalParameters;
    }

    public String getDeployMode() {
      return myDeployMode;
    }

    public void setDeployMode(String deployMode) {
      myDeployMode = deployMode;
    }

    public String getBaseDirectoryName() {
      return myBaseDirectoryName;
    }

    public void setBaseDirectoryName(String baseDirectoryName) {
      myBaseDirectoryName = baseDirectoryName;
    }
  }
}
