package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;

public abstract class AbstractDeploymentModelCommand<T> extends AbstractDMConnectorCommand<T> {

  private final DeploymentModel myDeploymentModel;

  public AbstractDeploymentModelCommand(DMServerInstance dmServer, DeploymentModel deploymentModel) {
    super(dmServer);
    myDeploymentModel = deploymentModel;
  }

  @Override
  protected T doExecute(MBeanServerConnection connection) throws JMException, IOException {
    DeploymentIdentity identity = getIdentity();
    if (identity == null) {
      return null;
    }
    return doDeploymentExecute(connection, identity);
  }

  protected DeploymentIdentity getIdentity() {
    return getServerInstance().findRegisteredDeployment(myDeploymentModel);
  }

  protected abstract T doDeploymentExecute(MBeanServerConnection connection, DeploymentIdentity identity) throws JMException, IOException;
}
