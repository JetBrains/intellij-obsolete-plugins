package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.common.MBeanUtil;
import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.deploy.jmx.ConnectorKernelStatusCommand;
import com.intellij.dmserver.deploy.jmx.QueryDeploymentStatusCommand;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.dmserver.run.DMServerStartupPolicy;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.ObjectName;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class ServerVersion2Base extends ServerVersion10 {

  private final String myRootPackage;

  private final ObjectName myMBeanDeployer;
  private final ObjectName myMBeanRecoveryMonitor;
  private final ObjectName myMBeanBundleAdmin;
  private final ObjectName myMBeanKernelStatus;

  protected ServerVersion2Base(@NonNls String rootPackage) {
    myRootPackage = rootPackage;
    myMBeanDeployer = MBeanUtil.newObjectName(rootPackage + ".kernel:category=Control,type=Deployer");
    myMBeanRecoveryMonitor = MBeanUtil.newObjectName(rootPackage + ".kernel:category=Control,type=RecoveryMonitor");
    myMBeanBundleAdmin = MBeanUtil.newObjectName(rootPackage + ".server:type=BundleAdmin");
    myMBeanKernelStatus = MBeanUtil.newObjectName(rootPackage + ".kernel:type=KernelStatus");
  }

  @Override
  @NotNull
  public DMVersion getVersion() {
    return DMVersion.DM_20;
  }

  @Override
  public ObjectName getDeployerMBean() {
    return myMBeanDeployer;
  }

  @Override
  public ObjectName getModelMBean(DeploymentIdentity identity) {
    StringBuilder nameBuilder = new StringBuilder(myRootPackage + ".kernel:type=" + getDeploymentMBeanType() + ","
                                                  + "artifact-type=" + identity.getType() + ","
                                                  + "name=" + identity.getSymbolicName() + ","
                                                  + "version=" + identity.getVersion());
    String region = identity.getRegion();
    if (region == null) {
      region = getDeploymentMBeanRegion();
    }
    if (region != null) {
      nameBuilder.append(",region=");
      nameBuilder.append(region);
    }

    return MBeanUtil.newObjectName(nameBuilder.toString());
  }

  protected String getDeploymentMBeanType() {
    return "Model";
  }

  @Nullable
  protected String getDeploymentMBeanRegion() {
    return null;
  }

  @Override
  public ObjectName getBundleAdminMBean() {
    return myMBeanBundleAdmin;
  }

  @Override
  public ObjectName getRecoveryMonitorMBean() {
    return myMBeanRecoveryMonitor;
  }

  @Override
  public ObjectName getKernelStatusMBean() {
    return myMBeanKernelStatus;
  }

  @Override
  public ObjectName getRepositoryMBean(String repositoryName) {
    return MBeanUtil.newObjectName(myRootPackage + ".kernel:type=Repository,name=" + repositoryName);
  }

  @Override
  public VirtualFile getRuntimeBaseFolder(DMServerInstallation
                                            installation) {
    return installation.getHome();
  }

  @Override
  public String getJmxScriptName() {
    return DMServerStartupPolicy.DMK_SCRIPT;
  }

  @Override
  public String getJmxPortEnvVar() {
    return "JMX_PORT";
  }

  @Override
  public boolean pingServerInstance(DMServerInstance serverInstance) throws ExecutionException, TimeoutException {
    Boolean result = new ConnectorKernelStatusCommand(serverInstance).execute();
    if (!Boolean.TRUE.equals(result)) {
      return false;
    }

    if (!isDeploymentReady(serverInstance, createAdminIdentity())) {
      return false;
    }

    DeploymentIdentity needForRepositoryIdentity = createNeedForRepositoryIdentity();
    if (needForRepositoryIdentity != null && !isDeploymentReady(serverInstance, needForRepositoryIdentity)) {
      return true;
    }

    DeploymentIdentity repositoryIdentity = createRepositoryIdentity();
    return repositoryIdentity == null || isDeploymentReady(serverInstance, repositoryIdentity);
  }

  private static boolean isDeploymentReady(DMServerInstance serverInstance, final DeploymentIdentity identity)
    throws TimeoutException, ExecutionException {
    Boolean ready = new QueryDeploymentStatusCommand(serverInstance, null) {

      @Override
      protected DeploymentIdentity getIdentity() {
        return identity;
      }
    }.execute();
    return Boolean.TRUE.equals(ready);
  }

  @Nullable
  protected DeploymentIdentity createNeedForRepositoryIdentity() {
    return null;
  }

  protected abstract DeploymentIdentity createAdminIdentity();

  @Nullable
  protected abstract DeploymentIdentity createRepositoryIdentity();

  @Override
  public String getFamilyName() {
    return DmServerBundle.message("DMServerHelper.server.family.name.2_0");
  }
}
