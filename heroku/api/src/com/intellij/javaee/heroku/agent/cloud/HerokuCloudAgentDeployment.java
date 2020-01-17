package com.intellij.javaee.heroku.agent.cloud;

import com.intellij.remoteServer.agent.util.CloudAgentLoggingHandler;
import com.intellij.remoteServer.agent.util.CloudGitAgentDeployment;
import com.intellij.remoteServer.agent.util.log.LogListener;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * @author michael.golubev
 */
public interface HerokuCloudAgentDeployment extends CloudGitAgentDeployment {

  CompletableFuture<LogListener> startListeningLog(CloudAgentLoggingHandler loggingHandler);

  void stopListeningLog();

  void deployWar(File file);

  void attachDebugRemote(String host, Integer port);

  void detachDebugRemote();

  void startBashSession();
}
