package com.intellij.javaee.heroku.cloud;

import com.intellij.execution.ExecutionException;
import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentDeployment;
import com.intellij.openapi.util.Computable;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.debug.JavaDebugConnectionData;
import com.intellij.remoteServer.runtime.deployment.debug.JavaDebugServerModeHandler;
import com.intellij.remoteServer.util.AgentTaskExecutor;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.remoteServer.util.ServerRuntimeException;
import org.jetbrains.annotations.Nullable;

/**
 * @author michael.golubev
 */
public class HerokuDebugConnectionDelegate extends JavaDebugServerModeHandler {

  private final DeploymentTask<? extends CloudDeploymentNameConfiguration> myDeploymentTask;
  private final HerokuCloudAgentDeployment myAgentDeployment;
  private final AgentTaskExecutor myAgentTaskExecutor;

  public HerokuDebugConnectionDelegate(DeploymentTask<? extends CloudDeploymentNameConfiguration> task,
                                       HerokuCloudAgentDeployment deployment, AgentTaskExecutor executor) {
    myDeploymentTask = task;
    myAgentDeployment = deployment;
    myAgentTaskExecutor = executor;
  }

  public JavaDebugConnectionData getDebugConnectionData() throws ServerRuntimeException {
    HerokuDeploymentConfiguration deploymentConfiguration = (HerokuDeploymentConfiguration)myDeploymentTask.getConfiguration();
    return new JavaDebugConnectionData("127.0.0.1", deploymentConfiguration.getDebugPort()) {

      @Override
      @Nullable
      public JavaDebugServerModeHandler getServerModeHandler() {
        return HerokuDebugConnectionDelegate.this;
      }
    };
  }

  @Override
  public void attachRemote() throws ExecutionException {
    final HerokuDeploymentConfiguration deploymentConfiguration = (HerokuDeploymentConfiguration)myDeploymentTask.getConfiguration();

    try {
      myAgentTaskExecutor.execute((Computable)() -> {
        myAgentDeployment.attachDebugRemote(deploymentConfiguration.getHost(), deploymentConfiguration.getDebugPort());
        return null;
      });
    }
    catch (ServerRuntimeException e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  public void detachRemote() throws ExecutionException {
    try {
      doDetachRemote();
    }
    catch (ServerRuntimeException e) {
      throw new ExecutionException(e);
    }
  }

  private void doDetachRemote() throws ServerRuntimeException {
    myAgentTaskExecutor.execute((Computable)() -> {
      myAgentDeployment.detachDebugRemote();
      return null;
    });
  }

  public void checkDebugMode() throws ServerRuntimeException {
    if (myDeploymentTask.isDebugMode()) {
      HerokuDeploymentConfiguration deploymentConfiguration = (HerokuDeploymentConfiguration)myDeploymentTask.getConfiguration();
      if (deploymentConfiguration.getDebugPort() == null) {
        throw new ServerRuntimeException("Debug port required");
      }
      if (deploymentConfiguration.getHost() == null) {
        throw new ServerRuntimeException("Debug host required");
      }
    }
    doDetachRemote();
  }
}
