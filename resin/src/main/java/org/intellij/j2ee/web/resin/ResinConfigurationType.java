package org.intellij.j2ee.web.resin;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.run.configuration.J2EEConfigurationFactory;
import com.intellij.javaee.appServers.run.configuration.JavaeeAppServerConfigurationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ResinConfigurationType extends JavaeeAppServerConfigurationType {
  public ResinConfigurationType() {
    super("ResinConfigurationType");
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return ResinBundle.message("run.config.tab.title.resin");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return ResinBundle.message("run.config.tab.description.resin");
  }

  @Override
  public Icon getIcon() {
    return ResinManager.ICON_RESIN;
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.ResinConfigurationType";
  }

  @Override
  public RunConfiguration createJ2EEConfigurationTemplate(final ConfigurationFactory factory, final Project project,
                                                          final boolean isLocal) {
    return J2EEConfigurationFactory.getInstance().createJ2EERunConfiguration(factory,
                                                                             project,
                                                                             isLocal ? new ResinModel() : new ResinRemoteModel(),
                                                                             ResinManager.getInstance(),
                                                                             isLocal,
                                                                             isLocal ? new ResinStartupPolicy() : null);
  }

  @Nullable
  @Override
  public AppServerIntegration getIntegration() {
    return ResinManager.getInstance();
  }

  public static ResinConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(ResinConfigurationType.class);
  }
}
