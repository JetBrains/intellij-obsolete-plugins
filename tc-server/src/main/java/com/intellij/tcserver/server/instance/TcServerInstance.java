package com.intellij.tcserver.server.instance;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.javaee.appServers.run.configuration.CommonStrategy;
import com.intellij.javaee.appServers.run.localRun.ExecutableObject;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.DefaultJ2EEServerEvent;
import com.intellij.javaee.appServers.serverInstances.DefaultServerInstance;
import com.intellij.javaee.util.ServerInstancePoller;
import com.intellij.javaee.web.debugger.engine.DefaultJSPPositionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.tcserver.deployment.JmxProvider;
import com.intellij.tcserver.deployment.TcServerDeploymentModel;
import com.intellij.tcserver.deployment.TcServerDeploymentProvider;
import com.intellij.tcserver.deployment.exceptions.FailedToConnectJmxException;
import com.intellij.tcserver.deployment.exceptions.NotAllowedToConnectException;
import com.intellij.tcserver.server.integration.TcServerData;
import com.intellij.tcserver.util.TcServerBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class TcServerInstance extends DefaultServerInstance {
  private static final Logger MY_LOG = Logger.getInstance(TcServerInstance.class);
  @NonNls private static final Pattern PING_SERVER_STARTUP_PATTERN = Pattern.compile("^Connected to server$");
  //wrapper  | SpringSource tc Runtime - tcruntime-C-opt-springsource-tc-server-developer-lenny started.
  @NonNls private static final Pattern WRAPPER_SERVER_STARTUP_PATTERN = Pattern.compile("^wrapper  \\| .* started.$");

  @NonNls private static final String WEBAPPS_ROOT_DIR = "ROOT";

  private ServerInstancePoller myPoller;

  public TcServerInstance(CommonModel runConfiguration) {
    super(runConfiguration);
  }

  private Pattern getServerStartedMessagePattern() {
    if (getCommonModel().isLocal() && SystemInfo.isWindows && !((TcServerModelBase)getServerModel()).isBatchMode()) {
      return WRAPPER_SERVER_STARTUP_PATTERN;
    }
    else {
      return PING_SERVER_STARTUP_PATTERN;
    }
  }


  @Override
  public void start(ProcessHandler processHandler) {
    super.start(processHandler);
    myPoller = new ServerInstancePoller();
    myPoller.onInstanceStart();

    final Pattern serverStartedMessagePattern = getServerStartedMessagePattern();

    processHandler.addProcessListener(new ProcessAdapter() {

      @Override
      public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {
        String text = processEvent.getText();
        if (text != null && serverStartedMessagePattern.matcher(text).find()) {
          JmxProvider jmxProvider = TcServerDeploymentProvider.getJmxProvider(TcServerInstance.this);

          /*
         JMX starts after message, and it usually takes ~120 attempts to get jmx on ubuntu.
         There is no such problem on windows.
          */
          int times = 0;
          while (times < 1000) {
            try {
              jmxProvider.ping();
              fireServerListeners(new DefaultJ2EEServerEvent(true, false));
              return;
            }
            catch (NotAllowedToConnectException e) {
              fireServerListeners(new DefaultJ2EEServerEvent(true, false));
              return;
            }
            catch (FailedToConnectJmxException e) {
              times++;
            }
          }
          fireServerListeners(new DefaultJ2EEServerEvent(true, false));
        }
      }
    });

    final Project project = getCommonModel().getProject();
    DebuggerManager.getInstance(project).addDebugProcessListener(processHandler, new DebugProcessListener() {
      @Override
      public void processAttached(@NotNull DebugProcess process) {
        process.appendPositionManager(new DefaultJSPPositionManager(process, getScopeFacets(getCommonModel())) {
          @Override
          protected String getGeneratedClassesPackage() {
            return "org.apache.jsp";
          }
        });
      }
    });
  }

  @Override
  public void shutdown() {
    if (myPoller != null) {
      myPoller.onInstanceShutdown();
    }

    if (getCommonModel().isLocal() && ((TcServerModelBase)getServerModel()).isBatchMode()) {
      JmxProvider jmxProvider = TcServerDeploymentProvider.getJmxProvider(this);
      jmxProvider.shutdownServer();
      notifyTextAvailable("Disconnected from server");
    }
  }

  public ServerInstancePoller getPoller() {
    return myPoller;
  }

  public void notifyTextAvailable(String text) {
    if (!StringUtil.isEmpty(text)) {
      getProcessHandler().notifyTextAvailable(text, ProcessOutputTypes.SYSTEM);
    }
  }

  @Override
  public boolean isStartupScriptTerminatesAfterServerStartup(@NotNull ExecutableObject startupScript) {
    return true;
  }

  @Override
  public void updateChangedFiles(final Set<String> changedFilesPaths) {
    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      application.executeOnPooledThread(() -> doSlowUpdateChangedFiles(changedFilesPaths));
    }
    else {
      doSlowUpdateChangedFiles(changedFilesPaths);
    }
  }

  private void doSlowUpdateChangedFiles(Set<String> changedFilesPaths) {
    CommonStrategy strategy = (CommonStrategy)getCommonModel();

    if (!getCommonModel().isLocal()) {
      return;
    }

    TcServerData serverData = (TcServerData)getCommonModel().getApplicationServer().getPersistentData();
    @NonNls String basePath = serverData.getSdkPath() + File.separator + serverData.getServerName() + File.separator + "webapps";

    final List<Artifact> artifactList = strategy.getDeployedArtifacts();
    for (Artifact artifact : artifactList) {
      final String outputPath = artifact.getOutputPath();
      if (outputPath == null) {
        continue;
      }

      final TcServerDeploymentModel deploymentModel = (TcServerDeploymentModel)getCommonModel().getDeploymentModel(artifact);
      if (deploymentModel == null) {
        continue;
      }

      String artifactOutputPath = FileUtil.toSystemDependentName(outputPath);
      hotDeployArtifact(artifactOutputPath, changedFilesPaths, basePath, deploymentModel.getWebPath());
    }

    if (!changedFilesPaths.isEmpty()) {
      notifyTextAvailable(TcServerBundle.datedMessage("serverInstance.resourcesUpdated"));
    }
  }

  private void hotDeployArtifact(String artifactOutputPath,
                                 Set<String> changedFilesPaths,
                                 String basePath,
                                 String webDeploymentPath) {

    if ("/".equals(webDeploymentPath)) {
      webDeploymentPath = WEBAPPS_ROOT_DIR;
    }
    final String prefix = basePath + File.separator + webDeploymentPath + File.separator;

    for (String path : changedFilesPaths) {
      if (FileUtil.startsWith(path, artifactOutputPath)) {
        String newPath = prefix + FileUtil.getRelativePath(artifactOutputPath, path, File.separatorChar);

        newPath = FileUtil.toSystemDependentName(newPath);

        try {
          FileUtil.copy(new File(path), new File(newPath));
        }
        catch (IOException e) {
          notifyTextAvailable(TcServerBundle.message("serverInstance.failedToCopy", path, newPath, e.getMessage()));
          MY_LOG.warn(e);
        }
      }
    }
  }
}

