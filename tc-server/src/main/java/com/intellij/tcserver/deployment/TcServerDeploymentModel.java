package com.intellij.tcserver.deployment;

import com.intellij.javaee.appServers.context.DeploymentModelContext;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

public class TcServerDeploymentModel extends DeploymentModel implements DeploymentModelContext {

  private static final String NOT_ALLOWED_SYMBOLS = "*,?:=";

  private static final String NOT_ALLOWED_EXTENSION = ".war";

  private String myWebPath;

  @NonNls
  private String myServerService = "Catalina";
  @NonNls
  private String myServerHost = "localhost";

  public TcServerDeploymentModel(CommonModel parentConfiguration, DeploymentSource source) {
    super(parentConfiguration, source);
    if (source != null) {
      myWebPath = getWebPath(source.getPresentableName());
    }
    else {
      myWebPath = "/";
    }
  }

  @Override
  public boolean isDefaultContextRoot() {
    return false;
  }

  @Override
  public String getContextRoot() {
    return getWebPath();
  }

  public static String validateWebPath(String path) throws ConfigurationException {
    if (StringUtil.isEmpty(path)) {
      return "/";
    }

    path = path.trim();

    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    int invalidSymbolPosition = StringUtil.findFirst(path, ch -> StringUtil.containsChar(NOT_ALLOWED_SYMBOLS, ch));

    String notAllowedPart = null;
    if (invalidSymbolPosition >= 0) {
      notAllowedPart = Character.toString(path.charAt(invalidSymbolPosition));
    }
    else if (StringUtil.endsWith(path, NOT_ALLOWED_EXTENSION)) {
      notAllowedPart = NOT_ALLOWED_EXTENSION;
    }
    if (!StringUtil.isEmpty(notAllowedPart)) {
      throw new ConfigurationException(TcServerBundle.message("deploymentModel.invalidPath", path, notAllowedPart));
    }
    return path;
  }

  private static String getWebPath(String artifactName) {
    return "/" + FileUtil.sanitizeFileName(artifactName);
  }

  public String getWebPath() {
    return myWebPath;
  }

  public void setWebPath(String webPath) {
    myWebPath = webPath;
  }

  public String getServerService() {
    return myServerService;
  }

  public void setServerService(String serverService) {
    myServerService = serverService;
  }

  public String getServerHost() {
    return myServerHost;
  }

  public void setServerHost(String serverHost) {
    myServerHost = serverHost;
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    ExtraDeploymentSettings settings = new ExtraDeploymentSettings(myWebPath, myServerService, myServerHost);
    XmlSerializer.serializeInto(settings, element, null);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    ExtraDeploymentSettings settings = new ExtraDeploymentSettings();
    XmlSerializer.deserializeInto(settings, element);
    myWebPath = settings.getWebPath();
    myServerService = settings.getServerService();
    myServerHost = settings.getServerHost();
  }


  @SuppressWarnings("UnusedDeclaration")
  public static class ExtraDeploymentSettings {
    private String myPath;
    private String myService;
    private String myHost;

    public ExtraDeploymentSettings() {
    }

    public ExtraDeploymentSettings(String myPath, String myServerService, String myServerHost) {
      this.myPath = myPath;
      this.myService = myServerService;
      this.myHost = myServerHost;
    }

    @Tag("webPath")
    public String getWebPath() {
      return myPath;
    }

    public void setWebPath(String webPath) {
      myPath = webPath;
    }

    @Tag("serverService")
    public String getServerService() {
      return myService;
    }

    public void setServerService(String serverService) {
      myService = serverService;
    }

    @Tag("serverHost")
    public String getServerHost() {
      return myHost;
    }

    public void setServerHost(String serverHost) {
      myHost = serverHost;
    }
  }
}
