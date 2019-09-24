package com.intellij.javaee.heroku.cloud;

import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgent;
import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentConfig;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.remoteServer.agent.util.CloudAgentLogger;
import com.intellij.remoteServer.agent.util.CloudRemoteApplication;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.remoteServer.util.CloudApplicationRuntime;
import com.intellij.remoteServer.util.CloudMultiSourceServerRuntimeInstance;
import com.intellij.remoteServer.util.CloudServerRuntimeInstance;
import com.intellij.remoteServer.util.ServerRuntimeException;
import com.intellij.remoteServer.util.ssh.SshKeyAwareServerRuntime;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author michael.golubev
 */
public class HerokuServerRuntimeInstance extends CloudMultiSourceServerRuntimeInstance<
  HerokuDeploymentConfiguration,
  HerokuCloudAgentConfig,
  HerokuCloudAgent,
  HerokuCloudConfiguration> implements SshKeyAwareServerRuntime {

  private static final String SPECIFICS_MODULE_NAME = "intellij.clouds.heroku.agent.impl.rt";

  private static final String SPECIFICS_JAR_PATH = "specifics/herokuSpecifics.jar";

  private final RemoteServer<HerokuCloudConfiguration> myServer;

  public HerokuServerRuntimeInstance(RemoteServer<HerokuCloudConfiguration> server,
                                     ServerTaskExecutor taskExecutor,
                                     List<File> clientLibrary)
    throws Exception {
    super(HerokuCloudType.getInstance(),
          server.getConfiguration(),
          taskExecutor,
          clientLibrary,
          Collections.emptyList(),
          SPECIFICS_MODULE_NAME,
          SPECIFICS_JAR_PATH,
          HerokuCloudAgent.class,
          "com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentImpl");
    myServer = server;
  }

  @NotNull
  @Override
  public String getDeploymentName(@NotNull DeploymentSource source, @NotNull HerokuDeploymentConfiguration configuration) {
    return configuration.getDeploymentSourceName(source);
  }

  @Override
  protected void doConnect(HerokuCloudConfiguration configuration, CloudAgentLogger logger) {
    getAgent().connect(configuration, getAgentTaskExecutor(), logger);
  }

  @Override
  public void addSshKey(File sshKey) throws ServerRuntimeException {
    final String sshKeyText;
    try {
      sshKeyText = FileUtil.loadFile(sshKey);
    }
    catch (IOException e) {
      throw new ServerRuntimeException(e);
    }
    addSshKey(sshKeyText);
  }

  public void addSshKey(final String sshKey) throws ServerRuntimeException {
    getAgentTaskExecutor().execute(() -> {
      getAgent().addSshKey(sshKey);
      return null;
    });
  }

  @Override
  public CloudServerRuntimeInstance asCloudServerRuntime() {
    return this;
  }

  @Override
  public RemoteServer getServer() {
    return myServer;
  }

  @Override
  protected CloudApplicationRuntime createApplicationRuntime(CloudRemoteApplication application) {
    return new HerokuApplicationRuntime(this, application.getName());
  }
}