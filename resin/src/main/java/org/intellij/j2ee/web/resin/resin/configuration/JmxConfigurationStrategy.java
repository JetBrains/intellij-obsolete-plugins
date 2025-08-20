package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.javaee.appServers.deployment.DeploymentStatus;
import com.intellij.openapi.util.Ref;
import org.intellij.j2ee.web.resin.ResinModelBase;
import org.intellij.j2ee.web.resin.resin.WebApp;
import org.jetbrains.annotations.NotNull;

public interface JmxConfigurationStrategy {

  boolean deployWithJmx(ResinModelBase resinModel, WebApp webApp);

  boolean undeployWithJmx(ResinModelBase resinModel, WebApp webApp);

  @NotNull
  DeploymentStatus getDeployStateWithJmx(ResinModelBase resinModel, WebApp webApp, Ref<Boolean> isFinal);
}
