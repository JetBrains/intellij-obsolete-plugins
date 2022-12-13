package com.intellij.dmserver.install;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.ObjectName;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface ServerVersionHandler {

  enum DMVersion {
    DM_10,
    DM_20,
  }

  ObjectName getDeployerMBean();

  @Nullable
  ObjectName getModelMBean(DeploymentIdentity identity);

  ObjectName getBundleAdminMBean();

  ObjectName getShutdownMBean();

  ObjectName getRecoveryMonitorMBean();

  ObjectName getKernelStatusMBean();

  @Nullable
  ObjectName getRepositoryMBean(String repositoryName);

  VirtualFile getRuntimeBaseFolder(DMServerInstallation installation);

  @NotNull
  DMVersion getVersion();

  String getFamilyName();

  DMServerConfigSupport createConfigSupport(VirtualFile home);

  String getJmxScriptName();

  String getJmxPortEnvVar();

  boolean pingServerInstance(DMServerInstance serverInstance) throws ExecutionException, TimeoutException;
}
