package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;

public class ConnectorForceCheckCommand extends AbstractDMConnectorCommand<Object> {
  @NonNls
  private static final String JMX_OP_FORCE_CHECK = "forceCheck";

  public ConnectorForceCheckCommand(DMServerInstance dmServer) {
    super(dmServer);
  }

  @Override
  protected Object doExecute(MBeanServerConnection connection) throws JMException, IOException {
    String repositoryName = getServerInstance().getRepositoryName();
    if (repositoryName == null) {
      return null;
    }
    ObjectName repositoryMBean = getServerVersion().getRepositoryMBean(repositoryName);
    if (repositoryMBean == null) {
      return null;
    }
    invokeOperation(connection, repositoryMBean, JMX_OP_FORCE_CHECK);
    return new Object();
  }
}
