package com.intellij.javaee.heroku.cloud;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.remoteServer.runtime.clientLibrary.ClientLibraryDescription;
import com.intellij.remoteServer.runtime.clientLibrary.ClientLibraryManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author michael.golubev
 */
public class HerokuConnector extends ServerConnector<HerokuDeploymentConfiguration> {
  private static final Logger LOG = Logger.getInstance(HerokuConnector.class);
  public static final ClientLibraryDescription HEROKU =
    new ClientLibraryDescription("heroku-173.3622", HerokuConnector.class.getResource("/resources/HerokuClientLib.xml"));
  private final RemoteServer<HerokuCloudConfiguration> myServer;
  private final ServerTaskExecutor myTasksExecutor;

  public HerokuConnector(RemoteServer<HerokuCloudConfiguration> server, ServerTaskExecutor tasksExecutor) {
    myServer = server;
    myTasksExecutor = tasksExecutor;
  }

  @Override
  public void connect(@NotNull final ConnectionCallback<HerokuDeploymentConfiguration> callback) {
    myTasksExecutor.submit(() -> {
      final List<File> clientLibrary;
      try {
        clientLibrary = ClientLibraryManager.getInstance().download(HEROKU);
      }
      catch (IOException e) {
        LOG.info(e);
        callback.errorOccurred("Failed to download client libraries: " + e.getMessage());
        return;
      }
      new HerokuServerRuntimeInstance(myServer, myTasksExecutor, clientLibrary).connect(callback);
    }, callback);
  }
}
