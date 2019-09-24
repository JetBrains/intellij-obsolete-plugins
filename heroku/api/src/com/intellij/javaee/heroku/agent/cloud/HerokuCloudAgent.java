package com.intellij.javaee.heroku.agent.cloud;

import com.intellij.remoteServer.agent.util.CloudAgentErrorHandler;
import com.intellij.remoteServer.agent.util.CloudAgentLogger;
import com.intellij.remoteServer.agent.util.CloudAgentLoggingHandler;
import com.intellij.remoteServer.agent.util.CloudGitAgent;

/**
 * @author michael.golubev
 */
public interface HerokuCloudAgent extends CloudGitAgent<HerokuCloudAgentConfig, HerokuCloudAgentDeployment> {

  @Override
  void connect(HerokuCloudAgentConfig config, CloudAgentErrorHandler errorHandler, CloudAgentLogger logger);

  void addSshKey(String sshKey);

  @Override
  HerokuCloudAgentDeployment createDeployment(String deploymentName, CloudAgentLoggingHandler loggingHandler);
}
