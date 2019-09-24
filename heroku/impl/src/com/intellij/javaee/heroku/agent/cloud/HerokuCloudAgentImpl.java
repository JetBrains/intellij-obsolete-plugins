package com.intellij.javaee.heroku.agent.cloud;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.connection.Connection;
import com.heroku.api.connection.JerseyClientAsyncConnection;
import com.heroku.api.exception.HerokuAPIException;
import com.intellij.javaee.heroku.agent.HerokuApplicationImpl;
import com.intellij.remoteServer.agent.util.CloudAgentErrorHandler;
import com.intellij.remoteServer.agent.util.CloudAgentLogger;
import com.intellij.remoteServer.agent.util.CloudAgentLoggingHandler;
import com.intellij.remoteServer.agent.util.CloudRemoteApplication;
import com.intellij.remoteServer.agent.util.log.LogAgentManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author michael.golubev
 */
public class HerokuCloudAgentImpl implements HerokuCloudAgent {

  private final LogAgentManager myLogManager = new LogAgentManager();

  private HerokuApiTaskProvider myTaskProvider;

  private CloudAgentLogger myLogger;
  private String myApiKey;

  private final List<HerokuCloudAgentDeploymentImpl> myCloudAgentDeployments = new ArrayList<>();

  @Override
  public void connect(HerokuCloudAgentConfig config, CloudAgentErrorHandler errorHandler, CloudAgentLogger logger) {
    try {
      myLogger = logger;

      Connection connection = new JerseyClientAsyncConnection();
      String apiKey = config.getApiKeySafe();
      myApiKey = apiKey;
      HerokuAPI api = new HerokuAPI(connection, apiKey);
      api.listApps();

      myTaskProvider = new HerokuApiTaskProvider(api, errorHandler, logger);
    }
    catch (HerokuAPIException e) {
      errorHandler.onError(e.toString());
    }
  }

  @Override
  public HerokuCloudAgentDeployment createDeployment(String deploymentName, CloudAgentLoggingHandler loggingHandler) {
    HerokuCloudAgentDeploymentImpl agentDeployment =
      new HerokuCloudAgentDeploymentImpl(myTaskProvider, deploymentName, loggingHandler, myLogManager, myLogger, myApiKey);
    myCloudAgentDeployments.add(agentDeployment);
    return agentDeployment;
  }

  @Override
  public void disconnect() {
    myLogManager.stopListeningAllLogs();
    stopAllDebugProcess();
  }

  public void stopAllDebugProcess() {
    myTaskProvider.new ApiTask() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException {
        for (HerokuCloudAgentDeploymentImpl agentDeployment : myCloudAgentDeployments) {
          agentDeployment.detachDebugRemote();
        }
        return null;
      }
    }.perform();
  }

  @Override
  public CloudRemoteApplication[] getApplications() {
    return myTaskProvider.new ApiTask<CloudRemoteApplication[]>() {

      @Override
      protected CloudRemoteApplication[] doPerform(HerokuAPI api) throws HerokuAPIException {
        List<CloudRemoteApplication> result = new ArrayList<>();
        for (App app : api.listApps()) {
          result.add(new HerokuApplicationImpl(app));
        }
        return result.toArray(new CloudRemoteApplication[0]);
      }
    }.perform();
  }

  private boolean isDeploymentMaintenance(final String deploymentName) {
    Boolean result = myTaskProvider.new ApiSilentTask<Boolean>() {

      @Override
      protected Boolean doPerform(HerokuAPI api) throws HerokuAPIException {
        return api.isMaintenanceModeEnabled(deploymentName);
      }
    }.perform();
    return result == null ? false : result;
  }

  @Override
  public void addSshKey(final String sshKey) {
    myTaskProvider.new ApiTask() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException {
        api.addKey(sshKey);
        return null;
      }
    }.perform();
  }
}
