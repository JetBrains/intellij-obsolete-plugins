package com.intellij.javaee.heroku.cloud;

import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentDeployment;
import com.intellij.remoteServer.agent.util.CloudGitApplication;
import com.intellij.remoteServer.agent.util.CloudRemoteApplication;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.debug.JavaDebugConnectionData;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.remoteServer.util.CloudDeploymentRuntime;
import com.intellij.remoteServer.util.CloudMultiSourceServerRuntimeInstance;
import com.intellij.remoteServer.util.ServerRuntimeException;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author michael.golubev
 */
public class HerokuArtifactDeploymentRuntime extends CloudDeploymentRuntime implements HerokuDebugConnectionProvider {

  private final File myArtifactFile;

  private final HerokuDebugConnectionDelegate myDebugConnectionDelegate;
  private final HerokuBashSessionHelper myBashSessionHelper;

  public HerokuArtifactDeploymentRuntime(CloudMultiSourceServerRuntimeInstance serverRuntime,
                                         DeploymentSource source,
                                         File artifactFile,
                                         DeploymentTask<? extends CloudDeploymentNameConfiguration> task,
                                         @Nullable DeploymentLogManager logManager)
    throws ServerRuntimeException {
    super(serverRuntime, source, task, logManager);
    myArtifactFile = artifactFile;
    myDebugConnectionDelegate = new HerokuDebugConnectionDelegate(task, getDeployment(), getAgentTaskExecutor());
    myBashSessionHelper = new HerokuBashSessionHelper(this);
  }

  @Override
  public HerokuCloudAgentDeployment getDeployment() {
    return (HerokuCloudAgentDeployment)super.getDeployment();
  }

  @Override
  public CloudRemoteApplication deploy() throws ServerRuntimeException {

    myDebugConnectionDelegate.checkDebugMode();
    final HerokuCloudAgentDeployment deployment = getDeployment();
    CloudGitApplication result = getAgentTaskExecutor().execute(() -> {
      deployment.deployWar(myArtifactFile);
      return deployment.findApplication();
    });
    if (result == null) {
      throw new ServerRuntimeException("Application not found");
    }
    deployment.startListeningLog(getLoggingHandler());
    // TODO: Bash console is temporarily unavailable
    //myBashSessionHelper.setupHyperlink();
    return result;
  }

  @Override
  public void undeploy() throws ServerRuntimeException {
    super.undeploy();
    getDeployment().stopListeningLog();
  }

  @Override
  public JavaDebugConnectionData getDebugConnectionData() throws ServerRuntimeException {
    return myDebugConnectionDelegate.getDebugConnectionData();
  }
}
