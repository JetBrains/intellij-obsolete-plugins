package org.intellij.j2ee.web.resin;

import com.intellij.javaee.appServers.appServerIntegrations.AppServerDeployedFileUrlProvider;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerHelper;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerUrlMapping;
import com.intellij.javaee.appServers.context.FacetContextProvider;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.openapi.ex.AppServerIntegrationsManager;
import com.intellij.javaee.web.WebFacetContextProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class ResinManager extends AppServerIntegration {
  private final ResinDeploymentProvider deploymentProvider = new ResinDeploymentProvider();
  public static final Icon ICON_RESIN = ResinIdeaIcons.Resin;
  private final ApplicationServerHelper resinApplicationServerHelper = new ResinApplicationServerHelper();

  private ApplicationServerUrlMapping myUrlMapping;

  @Override
  public Icon getIcon() {
    return ICON_RESIN;
  }

  @Override
  public String getPresentableName() {
    return ResinBundle.message("resin.application.server.name");
  }

  @Override
  public DeploymentProvider getDeploymentProvider(boolean local) {
    return local ? deploymentProvider : null;
  }

  public static ResinManager getInstance() {
    return AppServerIntegrationsManager.getInstance().getIntegration(ResinManager.class);
  }

  @Override
  public ApplicationServerHelper getApplicationServerHelper() {
    return resinApplicationServerHelper;
  }

  @NotNull
  @Override
  public AppServerDeployedFileUrlProvider getDeployedFileUrlProvider() {
    if (myUrlMapping == null) {
      myUrlMapping = new ApplicationServerUrlMapping() {

        @Override
        protected void collectFacetContextProviders(List<FacetContextProvider> facetContextProvider) {
          facetContextProvider.add(new WebFacetContextProvider());
        }
      };
    }
    return myUrlMapping;
  }
}
