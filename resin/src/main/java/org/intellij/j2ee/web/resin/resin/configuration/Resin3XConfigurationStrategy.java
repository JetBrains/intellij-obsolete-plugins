package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.javaee.appServers.deployment.DeploymentStatus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.intellij.j2ee.web.resin.ResinModelBase;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.intellij.j2ee.web.resin.resin.WebApp;
import org.intellij.j2ee.web.resin.resin.common.MBeanUtil;
import org.intellij.j2ee.web.resin.resin.jmx.ConnectorCommandBase;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Resin3XConfigurationStrategy extends ResinConfigurationStrategy implements JmxConfigurationStrategy {

  private static final Logger LOG = Logger.getInstance(Resin3XConfigurationStrategy.class);

  @NonNls
  protected static final String SERVER_ELEMENT = "server";
  @NonNls
  protected static final String SERVER_DEFAULT_ELEMENT = "server-default";
  @NonNls
  protected static final String HTTP = "http";
  @NonNls
  protected static final String PORT = "port";
  @NonNls
  protected static final String DIRTY_ATTR = "dirty";
  @NonNls
  protected static final String WEB_APP_ELEMENT = "web-app";
  @NonNls
  protected static final String ID = "id";
  @NonNls
  private static final String DOCUMENT_DIRECTORY_ATTR = "document-directory";
  @NonNls
  private static final String ARCHIVE_PATH_ATTR = "archive-path";
  @NonNls
  protected static final String HOST = "host";
  @NonNls
  protected static final String CHARSET = "character-encoding";
  @NonNls
  private static final String HOST_DEFAULT_ELEMENT = "host-default";
  @NonNls
  private static final String WEB_APP_DEPLOY_ELEMENT = "web-app-deploy";
  @NonNls
  private static final String ROOT_CONTEXT_PATH = "/";
  @NonNls
  private static final String ROOT_EXPAND_DIR = "ROOT";
  @NonNls
  private static final String DEFAULT_EXPAND_DIR = "webapps";
  @NonNls
  private static final String PATH_ATTR = "path";
  @NonNls
  private static final String REGEXP_ATTR = "regexp";

  @NonNls
  protected static final String RESIN_CONF = "resin3.conf";

  public static final ObjectName MBEAN_WEB_APP_DEPLOY = MBeanUtil.newObjectName("resin:type=WebAppDeploy,Host=default,name=webapps");

  @NonNls
  private static final String MBEAN_WEB_APP_PREFIX = "resin:type=WebApp,Host=default,name=/";

  @NonNls
  public static final String STATE_JMX_ATTRIBUTE = "State";
  @NonNls
  public static final String STATE_JMX_ATTRIBUTE_ACTIVE = "active";
  @NonNls
  public static final String STATE_JMX_ATTRIBUTE_ERROR = "error";
  @NonNls
  public static final String STATE_JMX_ATTRIBUTE_FAILED = "failed";

  private final ResinInstallation myResinInstallation;

  private final NotNullLazyValue<ElementsProvider> myElementsProvider = NotNullLazyValue.lazy(() -> {
    return createElementsProvider();
  });

  public Resin3XConfigurationStrategy(ResinInstallation resinInstallation) {
    myResinInstallation = resinInstallation;
  }

  protected final ResinInstallation getInstallation() {
    return myResinInstallation;
  }

  @Override
  public void setPort(final int port) {
    ElementsProvider elementsProvider = getElementsProvider();
    Element httpElement = elementsProvider.getOrCreateChildElement(elementsProvider.getParamParentElement(), HTTP);
    httpElement.setAttribute(PORT, Integer.toString(port));
  }

  @Override
  public boolean deploy(final WebApp webApp) throws ExecutionException {
    Ref<Boolean> dirty = new Ref<>(false);

    ElementsProvider elementsProvider = getElementsProvider();
    Namespace ns = elementsProvider.getNS();

    final Element host = getHost(webApp);
    removeAttribute(host, DIRTY_ATTR, dirty);

    String location = webApp.getLocation();
    boolean isExploded = new File(location).isDirectory();
    String contextPath = webApp.getContextPath();

    Element webAppEl = findWebAppElement(host, contextPath);
    if (webAppEl == null) {
      webAppEl = new Element(WEB_APP_ELEMENT, ns);
      webAppEl.setAttribute(ID, contextPath);
      host.addContent(webAppEl);
      dirty.set(true);
    }

    String expandDirAttr = getExpandDirAttr();
    setAttribute(webAppEl, isExploded ? expandDirAttr : ARCHIVE_PATH_ATTR, location, dirty);
    if (!isExploded) {
      StringBuilder expandDir = null;
      Element hostParent = elementsProvider.getHostParent();
      Element hostDefaultEl = hostParent.getChild(HOST_DEFAULT_ELEMENT, ns);
      if (hostDefaultEl != null) {
        Element webAppDeployEl = hostDefaultEl.getChild(WEB_APP_DEPLOY_ELEMENT, ns);
        if (webAppDeployEl != null) {
          String webAppDeployPath = webAppDeployEl.getAttributeValue(PATH_ATTR);
          if (StringUtil.isNotEmpty(webAppDeployPath)) {
            expandDir = new StringBuilder(webAppDeployPath);
          }
        }
      }
      if (expandDir == null) {
        expandDir = new StringBuilder(DEFAULT_EXPAND_DIR);
      }
      expandDir.append("/");
      expandDir.append(ROOT_CONTEXT_PATH.equals(contextPath)
                       ? ROOT_EXPAND_DIR
                       : FileUtil.sanitizeFileName(StringUtil.trimStart(contextPath, ROOT_CONTEXT_PATH)));
      setAttribute(webAppEl, expandDirAttr, expandDir.toString(), dirty, true);
    }

    String charset = webApp.getCharSet();
    if (charset == null || StringUtil.isEmptyOrSpaces(charset)) {
      removeAttribute(webAppEl, CHARSET, dirty);
    }
    else {
      setAttribute(webAppEl, CHARSET, charset.trim(), dirty);
    }

    return dirty.get();
  }

  protected String getExpandDirAttr() {
    return DOCUMENT_DIRECTORY_ATTR;
  }

  private static Element findWebAppElement(Element host, String contextPath) {
    final List webApps = host.getChildren(WEB_APP_ELEMENT, host.getNamespace());
    if (webApps != null) {
      for (Object webApp : webApps) {
        final Element webAppEl = (Element)webApp;
        if (webAppEl.getAttribute(ID).getValue().equals(contextPath)) {
          return webAppEl;
        }
      }
    }
    return null;
  }

  private static void setAttribute(Element element, String name, String value, Ref<Boolean> dirty) {
    setAttribute(element, name, value, dirty, false);
  }

  private static void setAttribute(Element element, String name, String value, Ref<Boolean> dirty, boolean keepExisting) {
    Attribute existingAttribute = element.getAttribute(name);
    if (existingAttribute == null || !keepExisting && !existingAttribute.getValue().equals(value)) {
      element.setAttribute(name, value);
      dirty.set(true);
    }
  }

  private static void removeAttribute(Element element, String name, Ref<Boolean> dirty) {
    if (element.getAttribute(name) != null) {
      element.removeAttribute(name);
      dirty.set(true);
    }
  }

  private Element getHost(final WebApp webApp) throws ExecutionException {
    try {
      ElementsProvider elementsProvider = getElementsProvider();
      Namespace ns = elementsProvider.getNS();

      Element hostParent = elementsProvider.getHostParent();

      final String webAppHost = webApp.getHost();

      final List hosts = hostParent.getChildren(HOST, ns);
      if (hosts != null) {
        for (Object host1 : hosts) {
          final Element host = (Element)host1;

          Attribute idAttribute = host.getAttribute(ID);
          if (idAttribute != null && StringUtil.equals(idAttribute.getValue(), webAppHost)) {
            return host;
          }

          Attribute regexpAttribute = host.getAttribute(REGEXP_ATTR);
          if (regexpAttribute != null && webAppHost.matches(regexpAttribute.getValue())) {
            return host;
          }
        }
      }

      // Not found, create a new one
      final Element host = new Element(HOST, ns);
      host.setAttribute(ID, webApp.getHost());
      host.setAttribute(DIRTY_ATTR, "true");
      hostParent.addContent(host);
      return host;
    }
    catch (Exception e) {
      throw new ExecutionException(ResinBundle.message("resin.conf.parse.error"), e);
    }
  }

  @Override
  public boolean undeploy(final WebApp webApp) {
    boolean dirty = false;

    ElementsProvider elementsProvider = getElementsProvider();

    final Namespace ns = elementsProvider.getNS();

    final List hosts = elementsProvider.getHostParent().getChildren(HOST, ns);
    if (hosts != null) {
      for (Object host : hosts) {
        final Element hostEl = (Element)host;
        Element webAppEl = findWebAppElement(hostEl, webApp.getContextPath());
        if (webAppEl != null) {
          hostEl.removeContent(webAppEl);
          dirty = true;
        }
      }
    }

    return dirty;
  }

  @Override
  public InputStream getDefaultResinConfContent() {
    return this.getClass().getResourceAsStream(RESIN_CONF);
  }

  @Override
  public boolean deployWithJmx(ResinModelBase resinModel, WebApp webApp) {
    final File webAppFile = new File(FileUtil.toSystemDependentName(webApp.getLocation()));
    if (!webAppFile.exists()) {
      LOG.error("Can't find web app");
      return false;
    }

    if (getDeployStateWithJmx(resinModel, webApp, new Ref<>()) != DeploymentStatus.UNKNOWN
        && !new UndeployCommand(resinModel, webAppFile).safeExecute()) {
      return false;
    }

    if (!cleanUpWebApp(resinModel, webAppFile)) {
      return true;
    }

    if (!resinModel.transferFile(webAppFile)) {
      return false;
    }

    if (!new DeployCommand(resinModel, "start", webAppFile.getName()).safeExecute()) {
      return false;
    }

    return true;
  }

  @Override
  @NotNull
  public DeploymentStatus getDeployStateWithJmx(ResinModelBase resinModel, WebApp webApp, Ref<Boolean> isFinal) {
    final File webAppFile = new File(FileUtil.toSystemDependentName(webApp.getLocation()));
    if (!webAppFile.exists()) {
      isFinal.set(true);
      return DeploymentStatus.FAILED;
    }

    GetStateCommand getStateCommand = new GetStateCommand(resinModel, webAppFile);
    if (!getStateCommand.safeExecute()) {
      isFinal.set(true);
      return DeploymentStatus.FAILED;
    }

    String state = getStateCommand.getResult();
    if (STATE_JMX_ATTRIBUTE_ACTIVE.equalsIgnoreCase(state)) {
      isFinal.set(true);
      return DeploymentStatus.DEPLOYED;
    }
    else if (STATE_JMX_ATTRIBUTE_ERROR.equalsIgnoreCase(state) || STATE_JMX_ATTRIBUTE_FAILED.equalsIgnoreCase(state)) {
      isFinal.set(true);
      return DeploymentStatus.FAILED;
    }

    return DeploymentStatus.UNKNOWN;
  }

  private static boolean cleanUpWebApp(ResinModelBase resinModel, File webAppFile) {
    if (!webAppFile.isDirectory()
        && !resinModel.deleteFile(new File(webAppFile.getParent(), FileUtilRt.getNameWithoutExtension(webAppFile.getName())))) {
      return false;
    }

    if (!resinModel.deleteFile(webAppFile)) {
      return false;
    }

    return true;
  }

  @Override
  public boolean undeployWithJmx(ResinModelBase resinModel, WebApp webApp) {
    final File webAppFile = new File(FileUtil.toSystemDependentName(webApp.getLocation()));
    if (!webAppFile.exists()) {
      LOG.error("Can't find web app");
      return false;
    }

    //if (!new DeployCommand(resinModel, "undeploy", getWebAppName(resinModel, webAppFile)).safeExecute()) { // "stop"
    //  return false;
    //}

    if (!new UndeployCommand(resinModel, webAppFile).safeExecute()) {
      return false;
    }

    if (!cleanUpWebApp(resinModel, webAppFile)) {
      return true;
    }

    GetStateCommand getStateCommand = new GetStateCommand(resinModel, webAppFile);
    if (!getStateCommand.safeExecute()) {
      return false;
    }

    return getStateCommand.getResult() == null;
  }

  private String getWebAppName(File webAppFile) {
    String fileName = webAppFile.getName();
    boolean trimExtension = !(webAppFile.isDirectory() && myResinInstallation.getVersion().getParsed().compare(4, 0, 10) > 0);
    return trimExtension ? FileUtilRt.getNameWithoutExtension(fileName) : fileName;
  }

  private static class DeployCommand extends ConnectorCommandBase<Object> {

    private final String myCommand;
    private final String myArg;

    DeployCommand(ResinModelBase resinModel, @NonNls String command, @NonNls String arg) {
      super(resinModel);
      myCommand = command;
      myArg = arg;
    }

    @Override
    protected Object doExecute(MBeanServerConnection connection) throws JMException, IOException {
      return invokeOperation(connection, MBEAN_WEB_APP_DEPLOY, myCommand, myArg);
    }
  }

  private abstract class WebAppCommandBase<T> extends ConnectorCommandBase<T> {

    private final ObjectName myObjectName;

    WebAppCommandBase(ResinModelBase resinModel, File webAppFile) {
      super(resinModel);
      myObjectName = MBeanUtil.newObjectName(MBEAN_WEB_APP_PREFIX + getWebAppName(webAppFile));
    }

    @Nullable
    @Override
    protected T doExecute(MBeanServerConnection connection) throws JMException, IOException {
      return doExecute(connection, myObjectName);
    }

    @Nullable
    protected abstract T doExecute(MBeanServerConnection connection, ObjectName objectName) throws JMException, IOException;
  }


  private class GetStateCommand extends WebAppCommandBase<String> {

    GetStateCommand(ResinModelBase resinModel, File webAppFile) {
      super(resinModel, webAppFile);
    }

    @Nullable
    @Override
    protected String doExecute(MBeanServerConnection connection, ObjectName objectName) throws JMException, IOException {
      try {
        return (String)connection.getAttribute(objectName, STATE_JMX_ATTRIBUTE);
      }
      catch (InstanceNotFoundException e) {
        return null;
      }
    }
  }

  private class UndeployCommand extends WebAppCommandBase<Boolean> {

    UndeployCommand(ResinModelBase resinModel, File webAppFile) {
      super(resinModel, webAppFile);
    }

    @Nullable
    @Override
    protected Boolean doExecute(MBeanServerConnection connection, ObjectName objectName) throws JMException, IOException {
      return invokeOperation(connection, objectName, "destroy");
    }
  }

  protected ElementsProvider createElementsProvider() {
    return new ElementsProvider(getElement());
  }

  protected final ElementsProvider getElementsProvider() {
    return myElementsProvider.getValue();
  }

  protected static class ElementsProvider {

    private final Element myRootElement;
    private final Namespace myNS;

    private final NullableLazyValue<Element> myServer = new NullableLazyValue<>() {

      @Override
      protected Element compute() {
        return getRootElement().getChild(SERVER_ELEMENT, getNS());
      }
    };
    private final NotNullLazyValue<Element> myParamParent = NotNullLazyValue.lazy(() -> {
      return doGetParamParent();
    });

    public ElementsProvider(Element element) {
      myRootElement = element;
      myNS = myRootElement.getNamespace();
    }

    protected final Element getRootElement() {
      return myRootElement;
    }

    public Namespace getNS() {
      return myNS;
    }

    private Element getOrCreateServerElement() {
      return getOrCreateChildElement(getRootElement(), SERVER_ELEMENT);
    }

    //Resin 3.0
    //      <root>
    //          <server>
    //              <host>
    public Element getHostParent() {
      return getOrCreateServerElement();
    }

    protected Element doGetParamParent() {
      return getOrCreateServerElement();
    }

    public Element getOrCreateChildElement(Element parentElement, @NonNls String childName) {
      Element result = parentElement.getChild(childName, getNS());
      if (result == null) {
        result = new Element(childName, getNS());
        parentElement.addContent(result);
      }
      return result;
    }

    @Nullable
    public Element getServerElement() {
      return myServer.getValue();
    }

    @NotNull
    public Element getParamParentElement() {
      return myParamParent.getValue();
    }
  }
}
