package com.intellij.dmserver.run;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.shell.DmShellToolWindowFactory;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.oss.server.JavaeeServerConnector;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class DMServerInstance extends JavaeeServerConnector {
  @NonNls
  private static final String TOOL_WINDOW_ID = "DM Shell";

  private final ServerVersionHandler myVersionHandler;

  private final Map<DeploymentModel, DeploymentIdentity> myDeploymentModel2Identity = new HashMap<>();

  public DMServerInstance(CommonModel commonModel, ServerVersionHandler versionHandler) {
    super(commonModel);
    myVersionHandler = versionHandler;
  }

  @NotNull
  @Override
  public DMServerModelBase getServerModel() {
    return (DMServerModelBase)super.getServerModel();
  }

  @Override
  public void start(ProcessHandler processHandler) {
    super.start(processHandler);
    processHandler.addProcessListener(new ProcessAdapter() {

      private final ToolWindowManager myToolWindowManager = ToolWindowManager.getInstance(getCommonModel().getProject());

      @Nullable
      private ToolWindow getShellToolWindow(boolean force) {
        ToolWindow window = myToolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        if (window == null && force) {
          window = myToolWindowManager.registerToolWindow(TOOL_WINDOW_ID, false, ToolWindowAnchor.RIGHT);
          window.setIcon(DmServerSupportIcons.DM);
          window.setAvailable(false);
        }
        return window;
      }

      @Override
      public void startNotified(@NotNull ProcessEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
          DmShellToolWindowFactory.addServer(getCommonModel(), getShellToolWindow(true), DMServerInstance.this);
        });
      }

      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (getCommonModel().getProject().isDisposed()) {
            return;
          }
          ToolWindow window = getShellToolWindow(false);
          if (window != null) {
            DmShellToolWindowFactory.removeServer(window, DMServerInstance.this);
          }
        });
      }
    });
  }

  @Override
  protected boolean doConnect() {
    try {
      return myVersionHandler.pingServerInstance(this);
    }
    catch (TimeoutException | ExecutionException e) {
      return false;
    }
  }

  @Override
  protected void doDisconnect() {

  }

  public boolean addToRepository(List<VirtualFile> filesToDeploy) {
    return getServerModel().addToRepository(filesToDeploy);
  }

  public boolean removeFromRepository(List<VirtualFile> filesToUndeploy) {
    return getServerModel().removeFromRepository(filesToUndeploy);
  }

  @Nullable
  public String getRepositoryName() {
    return getServerModel().getRepositoryName();
  }

  public ServerVersionHandler getVersionHandler() {
    return myVersionHandler;
  }

  public void registerDeployment(DeploymentModel model, DeploymentIdentity identity) {
    myDeploymentModel2Identity.put(model, identity);
  }

  public DeploymentIdentity findRegisteredDeployment(DeploymentModel model) {
    return myDeploymentModel2Identity.get(model);
  }
}
