package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;

public class ConnectorPingCommand extends AbstractDMConnectorCommand<Boolean> {
  @NonNls
  private static final String JMX_ATTR_RECOVERY_COMPLETE = "RecoveryComplete";

  public ConnectorPingCommand(DMServerInstance dmServer) {
    super(dmServer);
  }

  @Override
  protected Boolean doExecute(MBeanServerConnection connection) throws JMException, IOException {
    return (Boolean)connection.getAttribute(getServerVersion().getRecoveryMonitorMBean(), JMX_ATTR_RECOVERY_COMPLETE);
  }
}
