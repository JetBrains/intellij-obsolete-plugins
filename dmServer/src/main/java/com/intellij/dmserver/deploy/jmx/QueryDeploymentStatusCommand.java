package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.deploy.DeploymentIdentity;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Set;

public class QueryDeploymentStatusCommand extends AbstractDeploymentModelCommand<Boolean> {

  @NonNls
  private static final String JMX_ATTR_STATE = "State";

  @NonNls
  private static final String JMX_ATTR_STATE_VALUE_ACTIVE = "ACTIVE";

  public QueryDeploymentStatusCommand(DMServerInstance dmServer, DeploymentModel deploymentModel) {
    super(dmServer, deploymentModel);
  }

  @Override
  protected Boolean doDeploymentExecute(MBeanServerConnection connection, DeploymentIdentity identity) throws JMException, IOException {
    ObjectName deploymentObjectName = getServerVersion().getModelMBean(identity);
    if (deploymentObjectName == null) {
      return null;
    }

    if (StringUtil.equals(identity.getVersion(), "*")) {
      Set<ObjectName> objectNames = connection.queryNames(deploymentObjectName, null);
      if (objectNames.isEmpty()) {
        return false;
      }
      deploymentObjectName = ContainerUtil.getFirstItem(objectNames);
    }

    try {
      return JMX_ATTR_STATE_VALUE_ACTIVE.equalsIgnoreCase((String)connection.getAttribute(deploymentObjectName, JMX_ATTR_STATE));
    }
    catch (InstanceNotFoundException e) {
      return false;
    }
  }
}
