package org.intellij.j2ee.web.resin.resin.common;

import com.intellij.javaee.appServers.deployment.DeploymentManager;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.deployment.DeploymentStatus;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.openapi.project.Project;

public abstract class DeploymentProviderEx extends DeploymentProvider {

  protected static void setDeploymentStatus(J2EEServerInstance instance, DeploymentModel model, DeploymentStatus status) {
    final CommonModel configuration = instance.getCommonModel();
    final Project project = configuration.getProject();
    DeploymentManager.getInstance(project).setDeploymentStatus(model, status, configuration, instance);
  }
}
