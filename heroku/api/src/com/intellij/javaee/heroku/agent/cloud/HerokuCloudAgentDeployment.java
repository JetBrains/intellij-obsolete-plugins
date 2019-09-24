package com.intellij.javaee.heroku.agent.cloud;

import com.intellij.remoteServer.agent.util.CloudAgentLoggingHandler;
import com.intellij.remoteServer.agent.util.CloudGitAgentDeployment;

import java.io.File;

/**
 * @author michael.golubev
 */
public interface HerokuCloudAgentDeployment extends CloudGitAgentDeployment {

  void startListeningLog(CloudAgentLoggingHandler loggingHandler);

  void stopListeningLog();

  void deployWar(File file);

  void attachDebugRemote(String host, Integer port);

  void detachDebugRemote();

  void startBashSession();
}
