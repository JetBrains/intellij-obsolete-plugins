package com.intellij.tcserver.server.instance;

import com.intellij.execution.configurations.LogFileOptions;
import com.intellij.execution.configurations.PredefinedLogFile;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.run.configuration.PredefinedLogFilesProvider;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.server.integration.TcServerData;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class TcServerLocalModel extends TcServerModelBase implements PredefinedLogFilesProvider {
  @NonNls
  private static final String TOMCAT_LOCALHOST_LOG_ID = "TC_SERVER_TOMCAT_LOCALHOST_LOG_ID";
  @NonNls
  private static final String TOMCAT_CATALINA_LOG_ID = "TC_SERVER_TOMCAT_CATALINA_LOG_ID";

  @Override
  public String prepareDeployment(String sourcePath) {
    return sourcePath;
  }

  @Override
  protected void read(@NotNull Element element, boolean isPersistent) throws InvalidDataException {
    final TcServerModelLocalSettings settings = new TcServerModelLocalSettings();

    XmlSerializer.deserializeInto(settings, element);

    readFromSettingsBase(settings, isPersistent);
  }

  @Override
  protected void write(@NotNull Element element, boolean isPersistent) throws WriteExternalException {
    final TcServerModelLocalSettings settings = new TcServerModelLocalSettings();

    writeToSettingsBase(settings, isPersistent);

    XmlSerializer.serializeInto(settings, element, new SkipDefaultValuesSerializationFilters());
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new TcServerRunConfigurationEditor();
  }

  @Override
  public PredefinedLogFile @NotNull [] getPredefinedLogFiles() {
    return new PredefinedLogFile[]{
      new PredefinedLogFile(TOMCAT_LOCALHOST_LOG_ID, true),
      new PredefinedLogFile(TOMCAT_CATALINA_LOG_ID, true)
    };
  }

  @Override
  @Nullable
  public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile) {
    ApplicationServer applicationServer = getCommonModel().getApplicationServer();
    if (applicationServer == null) {
      return null;
    }
    TcServerData info = (TcServerData)applicationServer.getPersistentData();

    if (TOMCAT_LOCALHOST_LOG_ID.equals(predefinedLogFile.getId())) {
      String path = TcServerUtil.removeTrailingSeparators(info.getSdkPath());
      @NonNls final String hostLogFilePattern =
        path + File.separator + info.getServerName() + File.separator + "logs" + File.separator + "localhost*.log";
      return new LogFileOptions(TcServerBundle.message("localModel.tomcatLocalhostLogAlias"), hostLogFilePattern,
                                predefinedLogFile.isEnabled());
    }
    if (TOMCAT_CATALINA_LOG_ID.equals(predefinedLogFile.getId())) {
      String path = TcServerUtil.removeTrailingSeparators(info.getSdkPath());
      @NonNls final String hostLogFilePattern =
        path + File.separator + info.getServerName() + File.separator + "logs" + File.separator + "catalina*.log";
      return new LogFileOptions(TcServerBundle.message("localModel.tomcatCatalinaLogAlias"), hostLogFilePattern,
                                predefinedLogFile.isEnabled());
    }
    return null;
  }

  public static class TcServerModelLocalSettings extends TcServerModelSettingsBase {

  }
}
