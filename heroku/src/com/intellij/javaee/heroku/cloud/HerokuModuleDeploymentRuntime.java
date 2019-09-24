package com.intellij.javaee.heroku.cloud;


import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentDeployment;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.agent.util.CloudGitApplication;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.debug.JavaDebugConnectionData;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.remoteServer.util.CloudGitDeploymentRuntime;
import com.intellij.remoteServer.util.CloudMultiSourceServerRuntimeInstance;
import com.intellij.remoteServer.util.ServerRuntimeException;
import com.intellij.remoteServer.util.ssh.SshKeyChecker;

import java.io.*;
import java.util.Properties;

/**
 * @author michael.golubev
 */
public class HerokuModuleDeploymentRuntime extends CloudGitDeploymentRuntime implements HerokuDebugConnectionProvider {

  private static final String REMOTE_NAME = "heroku";
  private static final String CLOUD_NAME = "Heroku";
  private static final String SYSTEM_PROPERTY_FILE_NAME = "system.properties";
  private static final String JAVA_RUNTIME_NAME = "java.runtime.version";
  private static final String JAVA_RUNTIME_VALUE = "1.7";

  private final HerokuDebugConnectionDelegate myDebugConnectionDelegate;
  private final HerokuBashSessionHelper myBashSessionHelper;

  public HerokuModuleDeploymentRuntime(CloudMultiSourceServerRuntimeInstance serverRuntime,
                                       DeploymentSource source,
                                       File repositoryRoot,
                                       DeploymentTask<? extends CloudDeploymentNameConfiguration> task,
                                       DeploymentLogManager logManager) throws ServerRuntimeException {
    super(serverRuntime, source, repositoryRoot, task, logManager, REMOTE_NAME, CLOUD_NAME);
    myDebugConnectionDelegate = new HerokuDebugConnectionDelegate(task, getDeployment(), getAgentTaskExecutor());
    myBashSessionHelper = new HerokuBashSessionHelper(this);
  }

  @Override
  public HerokuCloudAgentDeployment getDeployment() {
    return (HerokuCloudAgentDeployment)super.getDeployment();
  }

  @Override
  public CloudGitApplication deploy() throws ServerRuntimeException {
    try {

      myDebugConnectionDelegate.checkDebugMode();
      CloudGitApplication result = super.deploy();
      getDeployment().startListeningLog(getLoggingHandler());
      // TODO: Bash console is temporarily unavailable
      //myBashSessionHelper.setupHyperlink();
      return result;
    }
    catch (ServerRuntimeException e) {
      new SshKeyChecker().checkDeploymentError(e.getMessage(),
                                               ((HerokuServerRuntimeInstance)getServerRuntime()),
                                               getLogManager(),
                                               getTask());
      throw e;
    }
  }

  @Override
  public void undeploy() throws ServerRuntimeException {
    super.undeploy();
    getDeployment().stopListeningLog();
  }

  public void createApplicationByTemplate(String templateGitUrl) throws ServerRuntimeException {
    new CloneJob().cloneToModule(templateGitUrl);
    fixJavaRuntime();
    CloudGitApplication application = createApplication();
    addGitRemote(application);
    fetchAndRefresh();
  }

  public void fixJavaRuntime() throws ServerRuntimeException {
    try {
      Properties systemProperties = new Properties();
      File systemPropertyFile = new File(getRepositoryRootFile(), SYSTEM_PROPERTY_FILE_NAME);

      if (systemPropertyFile.exists()) {
        FileInputStream inputStream = new FileInputStream(systemPropertyFile);
        try {
          systemProperties.load(inputStream);
        }
        finally {
          try {
            inputStream.close();
          }
          catch (IOException ignored) {

          }
        }
        if (StringUtil.equals(systemProperties.getProperty(JAVA_RUNTIME_NAME), JAVA_RUNTIME_VALUE)) {
          return;
        }
      }
      systemProperties.setProperty(JAVA_RUNTIME_NAME, JAVA_RUNTIME_VALUE);

      OutputStream outputStream = new FileOutputStream(systemPropertyFile);
      try {
        systemProperties.store(outputStream, null);
      }
      finally {
        try {
          outputStream.close();
        }
        catch (IOException ignored) {

        }
      }

      add();
      commit("Fix Java runtime version");
    }
    catch (IOException e) {
      throw new ServerRuntimeException(e);
    }
  }

  @Override
  public JavaDebugConnectionData getDebugConnectionData() throws ServerRuntimeException {
    return myDebugConnectionDelegate.getDebugConnectionData();
  }
}
