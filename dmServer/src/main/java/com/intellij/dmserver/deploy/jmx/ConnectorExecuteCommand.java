package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.DMConstants;
import com.intellij.dmserver.run.DMServerInstance;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;

public class ConnectorExecuteCommand extends AbstractBundleAdminCommand<String> {
  private final String myCommandLine;
  @NonNls
  private static final String JMX_OP_EXECUTE = "execute";

  public ConnectorExecuteCommand(DMServerInstance dmServer, String commandLine) {
    super(dmServer);
    myCommandLine = commandLine;
  }

  @Override
  protected String doExecute(MBeanServerConnection connection) throws JMException, IOException {
    checkBundleAdminAndInstall(connection);
    return invokeOperation(connection, DMConstants.MBEAN_BUNDLE_ADMIN, JMX_OP_EXECUTE, myCommandLine);
  }
}
