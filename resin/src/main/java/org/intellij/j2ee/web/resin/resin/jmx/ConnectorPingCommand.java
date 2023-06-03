package org.intellij.j2ee.web.resin.resin.jmx;

import org.intellij.j2ee.web.resin.ResinModelBase;
import org.intellij.j2ee.web.resin.resin.configuration.Resin3XConfigurationStrategy;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;

public class ConnectorPingCommand extends ConnectorCommandBase<Boolean> {

  private final int myJmxPort;

  public ConnectorPingCommand(ResinModelBase resinModel, int jmxPort) {
    super(resinModel);
    myJmxPort = jmxPort;
  }

  @Override
  protected int getJmxPort() {
    return myJmxPort;
  }

  // TODO: check behavior if ping is insuccessful - ex should not be logged
  @Override
  protected Boolean doExecute(MBeanServerConnection connection) throws JMException, IOException {
    // TODO: use server version - like DM: getServerVersion().getRecoveryMonitorMBean()
    return Resin3XConfigurationStrategy.STATE_JMX_ATTRIBUTE_ACTIVE.equalsIgnoreCase(
      (String)connection.getAttribute(Resin3XConfigurationStrategy.MBEAN_WEB_APP_DEPLOY, Resin3XConfigurationStrategy.STATE_JMX_ATTRIBUTE));
  }
}
