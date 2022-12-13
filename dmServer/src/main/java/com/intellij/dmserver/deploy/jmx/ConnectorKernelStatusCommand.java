package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;

public class ConnectorKernelStatusCommand extends AbstractDMConnectorCommand<Boolean> {
  @NonNls
  private static final String JMX_ATTR_STATUS = "Status";

  @NonNls
  private static final String JMX_ATTR_STATUS_STARTED = "STARTED";

  public ConnectorKernelStatusCommand(DMServerInstance dmServer) {
    super(dmServer);
  }

  @Override
  protected Boolean doExecute(MBeanServerConnection connection) throws JMException, IOException {
    return JMX_ATTR_STATUS_STARTED.equalsIgnoreCase(
      (String)connection.getAttribute(getServerVersion().getKernelStatusMBean(), JMX_ATTR_STATUS));
  }
}
