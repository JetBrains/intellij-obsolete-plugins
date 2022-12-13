package com.intellij.dmserver.run;

import com.intellij.dmserver.integration.DMServerIntegration;
import com.intellij.dmserver.run.remote.DMServerRemoteModel;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.run.configuration.JavaeeAppServerConfigurationType;
import com.intellij.javaee.appServers.run.configuration.ServerModel;
import com.intellij.javaee.appServers.run.localRun.ExecutableObjectStartupPolicy;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.openapi.project.Project;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class DMServerRunConfigurationType extends JavaeeAppServerConfigurationType {
  public DMServerRunConfigurationType() {
    super("com.intellij.dmserver.run.DMServerRunConfigurationType:id");
  }

  @NotNull
  public static DMServerRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(DMServerRunConfigurationType.class);
  }

  @NotNull
  @Override
  protected List<ConfigurationFactory> computeFactories() {
    List<ConfigurationFactory> result = new ArrayList<>();
    result.add(new DMConfigurationFactory(this, DmServerBundle.message("DMServerRunConfigurationType.name.local"), "Spring dmServer (Local)", true) {
      @Override
      protected ServerModel createServerModel() {
        return new DMServerModel();
      }

      @Override
      protected ExecutableObjectStartupPolicy createStartupPolicy() {
        return new DMServerStartupPolicy();
      }
    });

    if (TransportManager.getInstance().hasServices()) {
      result.add(new DMConfigurationFactory(this,
                                            DmServerBundle.message(
                                              "RemoteConfigurationFactoryAccess.configuration.name.remote"),
                                            "Spring dmServer (Remote)",
                                            false) {
        @Override
        protected ServerModel createServerModel() {
          return new DMServerRemoteModel();
        }

        @Override
        protected ExecutableObjectStartupPolicy createStartupPolicy() {
          return null;
        }
      });
    }
    return result;
  }

  @Override
  public RunConfiguration createJ2EEConfigurationTemplate(ConfigurationFactory factory, Project project, boolean isLocal) {
    //we have to extend J2EEConfigurationType, and thus implement this method
    //however we are using our own configuration factories that never calls this method
    throw new UnsupportedOperationException("This method should not be called");
  }

  @NotNull
  @Override
  public String getTag() {
    return "dmServer";
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.com.intellij.dmserver.run.DMServerRunConfigurationType:id";
  }

  @Override
  public AppServerIntegration getIntegration() {
    return DMServerIntegration.getInstance();
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return DmServerBundle.message("DMServerRunConfigurationType.display.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return DmServerBundle.message("DMServerRunConfigurationType.description");
  }

  @Override
  public Icon getIcon() {
    return DmServerSupportIcons.DM;
  }
}
