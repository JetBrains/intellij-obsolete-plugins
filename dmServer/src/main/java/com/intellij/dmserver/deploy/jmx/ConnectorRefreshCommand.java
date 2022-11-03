package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;

public class ConnectorRefreshCommand extends AbstractDeploymentModelCommand<Boolean> {
  @NonNls
  private static final String JMX_OP_REFRESH = "refresh";

  public ConnectorRefreshCommand(DMServerInstance dmServer, DeploymentModel deploymentModel) {
    super(dmServer, deploymentModel);
  }

  @Override
  protected Boolean doDeploymentExecute(MBeanServerConnection connection, DeploymentIdentity identity) throws JMException, IOException {
    return invokeOperation(connection, getServerVersion().getDeployerMBean(), JMX_OP_REFRESH, identity.getSymbolicName());
  }
}