package com.intellij.tcserver.server.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.run.configuration.JavaeeAppServerConfigurationType;
import com.intellij.javaee.appServers.run.configuration.ServerModel;
import com.intellij.javaee.appServers.run.localRun.ExecutableObjectStartupPolicy;
import com.intellij.javaee.transport.TransportManager;
import com.intellij.openapi.project.Project;
import com.intellij.tcserver.server.instance.TcServerLocalModel;
import com.intellij.tcserver.server.instance.remote.TcServerRemoteModel;
import com.intellij.tcserver.server.integration.TcServerIntegration;
import com.intellij.tcserver.util.TcServerBundle;
import icons.JavaeeAppServersTcServerIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class TcServerConfigurationType extends JavaeeAppServerConfigurationType {
  public TcServerConfigurationType() {
    super("tcServerDev");
  }

  @NotNull
  public static TcServerConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(TcServerConfigurationType.class);
  }

  @Override
  public RunConfiguration createJ2EEConfigurationTemplate(ConfigurationFactory factory, Project project, boolean isLocal) {
    throw new UnsupportedOperationException("ConfigurationFactory is used instead");
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return TcServerBundle.message("configurationType.displayName");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return TcServerBundle.message("configurationType.configurationTypeDescription");
  }

  @Override
  public Icon getIcon() {
    return JavaeeAppServersTcServerIcons.Browser_logo_sts_16;
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.tcServerDev";
  }

  @NotNull
  @Override
  protected List<ConfigurationFactory> computeFactories() {
    List<ConfigurationFactory> result = new ArrayList<>();
    result.add(new TcConfigurationFactory(this, TcServerBundle.message("TcServerConfigurationType.name.local"),
                                          "Spring tc Server (Local)", true
    ) {
      @Override
      protected ServerModel createServerModel() {
        return new TcServerLocalModel();
      }

      @Override
      protected ExecutableObjectStartupPolicy createExecutableObjectServiceStartupPolicy() {
        return new TcServerExecutableObjectStartupPolicy();
      }
    });

    if (TransportManager.getInstance().hasServices()) {
      result.add(new TcConfigurationFactory(this, TcServerBundle.message("TcServerConfigurationType.name.remove"),
                                            "Spring tc Server (Remote)", false
      ) {
        @Override
        protected ServerModel createServerModel() {
          return new TcServerRemoteModel();
        }

        @Override
        protected ExecutableObjectStartupPolicy createExecutableObjectServiceStartupPolicy() {
          return null;
        }
      });
    }
    return result;
  }

  @Nullable
  @Override
  public AppServerIntegration getIntegration() {
    return TcServerIntegration.getInstance();
  }
}
