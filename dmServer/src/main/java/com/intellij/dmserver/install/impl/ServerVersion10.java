package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.common.MBeanUtil;
import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.deploy.jmx.ConnectorPingCommand;
import com.intellij.dmserver.install.DMServerConfigSupport;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.dmserver.run.DMServerStartupPolicy;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.ObjectName;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author michael.golubev
 */
public class ServerVersion10 implements ServerVersionHandler {
  private static final ObjectName MBEAN_DEPLOYER_10 = MBeanUtil.newObjectName("com.springsource.server:type=Deployer");
  private static final ObjectName MBEAN_RECOVERY_MONITOR_10 = MBeanUtil.newObjectName("com.springsource.server:type=BundleAdmin");
  private static final ObjectName MBEAN_SHUTDOWN_10 = MBeanUtil.newObjectName("com.springsource.server:type=Shutdown");
  private static final ObjectName MBEAN_BUNDLE_ADMIN_10 = MBeanUtil.newObjectName("com.springsource.server:type=RecoveryMonitor");
  @NonNls private static final String TRACE_LOG_FILE_NAME = "trace.log";

  @Override
  @NotNull
  public DMVersion getVersion() {
    return DMVersion.DM_10;
  }

  @Override
  public ObjectName getDeployerMBean() {
    return MBEAN_DEPLOYER_10;
  }

  @Override
  public ObjectName getModelMBean(DeploymentIdentity identity) {
    return null;
  }

  @Override
  public ObjectName getBundleAdminMBean() {
    return MBEAN_BUNDLE_ADMIN_10;
  }

  @Override
  public ObjectName getShutdownMBean() {
    return MBEAN_SHUTDOWN_10;
  }

  @Override
  public ObjectName getRecoveryMonitorMBean() {
    return MBEAN_RECOVERY_MONITOR_10;
  }

  @Override
  public ObjectName getKernelStatusMBean() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public ObjectName getRepositoryMBean(String repositoryName) {
    return null;
  }

  @Override
  public VirtualFile getRuntimeBaseFolder(DMServerInstallation installation) {
    return installation.getHome();
  }

  @Override
  public DMServerConfigSupport createConfigSupport(VirtualFile home) {
    return new DMServerConfigSupport10(home);
  }

  @Override
  public String getJmxScriptName() {
    return DMServerStartupPolicy.STARTUP_SCRIPT;
  }

  @Override
  public String getJmxPortEnvVar() {
    return SystemInfo.isWindows ? "JMXPORT" : "jmxPort";
  }

  @Override
  public boolean pingServerInstance(DMServerInstance serverInstance) throws ExecutionException, TimeoutException {
    return new ConnectorPingCommand(serverInstance).execute() != null;
  }

  @Override
  public String getFamilyName() {
    return DmServerBundle.message("DMServerHelper.server.family.name.1_0");
  }
}