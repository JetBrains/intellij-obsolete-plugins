package com.intellij.javaee.heroku.cloud;

import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentDeployment;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.agent.util.CloudAgentLoggingHandler;
import com.intellij.remoteServer.util.CloudGitApplicationRuntime;
import com.intellij.remoteServer.util.CloudMultiSourceServerRuntimeInstance;

public class HerokuApplicationRuntime extends CloudGitApplicationRuntime {

  public HerokuApplicationRuntime(CloudMultiSourceServerRuntimeInstance serverRuntime, String applicationName) {
    super(serverRuntime, applicationName, null);
  }

  @Override
  public HerokuCloudAgentDeployment getDeployment() {
    return (HerokuCloudAgentDeployment)super.getDeployment();
  }

  public void showLog(final Project project, final Runnable onDone) {
    new LoggingTask() {

      @Override
      protected void run(CloudAgentLoggingHandler loggingHandler) {
        getDeployment().startListeningLog(loggingHandler).thenRun(onDone);
      }
    }.perform(project, () -> {});
  }
}
