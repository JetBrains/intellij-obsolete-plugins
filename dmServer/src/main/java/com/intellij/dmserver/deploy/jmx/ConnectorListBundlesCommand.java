package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.DMConstants;
import com.intellij.dmserver.run.DMServerInstance;
import com.springsource.server.management.remote.Bundle;
import org.jetbrains.annotations.NonNls;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.Map;

public class ConnectorListBundlesCommand extends AbstractBundleAdminCommand<Map<Long, Bundle>> {
  @NonNls
  private static final String JMX_OP_RETRIEVE_BUNDLES = "retrieveBundles";

  public ConnectorListBundlesCommand(DMServerInstance dmServer) {
    super(dmServer);
  }

  @Override
  protected Map<Long, Bundle> doExecute(MBeanServerConnection connection) throws JMException, IOException {
    checkBundleAdminAndInstall(connection);
    return invokeOperation(connection, DMConstants.MBEAN_BUNDLE_ADMIN, JMX_OP_RETRIEVE_BUNDLES);
  }
}
