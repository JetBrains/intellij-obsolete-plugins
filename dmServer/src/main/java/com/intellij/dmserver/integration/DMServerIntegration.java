package com.intellij.dmserver.integration;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerDeployedFileUrlProvider;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerUrlMapping;
import com.intellij.javaee.appServers.context.FacetContextProvider;
import com.intellij.javaee.appServers.openapi.ex.AppServerIntegrationsManager;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager;
import com.intellij.javaee.web.WebFacetContextProvider;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class DMServerIntegration extends AppServerIntegration {
  private final DMServerHelper myHelper;
  private ApplicationServerUrlMapping myUrlMapping;

  public static DMServerIntegration getInstance() {
    return AppServerIntegrationsManager.getInstance().getIntegration(DMServerIntegration.class);
  }

  public DMServerIntegration() {
    myHelper = new DMServerHelper();
  }

  @Override
  public Icon getIcon() {
    return DmServerSupportIcons.DM;
  }

  @Override
  public String getPresentableName() {
    return DmServerBundle.message("DMServerIntegration.name");
  }

  @Override
  public DMServerHelper getApplicationServerHelper() {
    return myHelper;
  }

  @Nullable
  public DMServerInstallation getServerInstallation(CommonModel commonModel) {
    ApplicationServer server = commonModel.getApplicationServer();
    return server == null ? null : getServerInstallation(server);
  }

  public DMServerInstallation getServerInstallation(ApplicationServer applicationServer) {
    if (applicationServer.getSourceIntegration() != this) {
      return null;
    }
    DMServerIntegrationData dataImpl = (DMServerIntegrationData)applicationServer.getPersistentData();
    return dataImpl.getInstallation();
  }

  public List<ApplicationServer> getDMServers() {
    ApplicationServersManager serversManager = ApplicationServersManager.getInstance();
    return serversManager.getApplicationServers(getInstance());
  }

  @Override
  @NotNull
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
