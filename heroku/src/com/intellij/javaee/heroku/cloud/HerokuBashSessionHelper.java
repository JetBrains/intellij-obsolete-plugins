package com.intellij.javaee.heroku.cloud;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentDeployment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Computable;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import com.intellij.remoteServer.util.CloudDeploymentRuntime;
import com.intellij.remoteServer.util.ServerRuntimeException;

/**
 * @author michael.golubev
 */
public class HerokuBashSessionHelper {

  private final CloudDeploymentRuntime myDeploymentRuntime;

  public HerokuBashSessionHelper(CloudDeploymentRuntime deploymentRuntime) {
    myDeploymentRuntime = deploymentRuntime;
  }

  public void setupHyperlink() {
    DeploymentLogManager myLogManager = myDeploymentRuntime.getLogManager();
    if (myLogManager == null) {
      return;
    }
    LoggingHandler loggingHandler = myLogManager.getMainLoggingHandler();
    loggingHandler.print("Access your application using ");
    loggingHandler.printHyperlink("Bash session...", new HyperlinkInfo() {

      @Override
      public void navigate(final Project project) {
        startBashSession();
      }
    });
    loggingHandler.print("\n");
  }

  private void startBashSession() {
    myDeploymentRuntime.getTaskExecutor().submit(() -> {
      try {
        myDeploymentRuntime.getAgentTaskExecutor().execute((Computable)() -> {
          ((HerokuCloudAgentDeployment)myDeploymentRuntime.getDeployment()).startBashSession();
          return null;
        });
      }
      catch (ServerRuntimeException e) {
        myDeploymentRuntime.getCloudNotifier().showMessage(e.getMessage(), MessageType.ERROR);
      }
    });
  }
}
