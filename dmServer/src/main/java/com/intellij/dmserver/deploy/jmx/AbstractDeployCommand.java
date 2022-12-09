package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.run.DMServerInstance;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.net.MalformedURLException;

public abstract class AbstractDeployCommand extends AbstractDMConnectorCommand<DeploymentIdentity> {
  @NonNls
  private static final String JMX_OP_DEPLOY = "deploy";

  public AbstractDeployCommand(DMServerInstance dmServer) {
    super(dmServer);
  }

  @Override
  protected final DeploymentIdentity doExecute(MBeanServerConnection connection) throws JMException, IOException {
    CompositeData result = invokeOperation(connection, getServerVersion().getDeployerMBean(), JMX_OP_DEPLOY, getUri(), false);
    //TODO: register somewhere (?)
    return result == null ? null : new DeploymentIdentity(result);
  }

  protected abstract String getUri() throws MalformedURLException;
}