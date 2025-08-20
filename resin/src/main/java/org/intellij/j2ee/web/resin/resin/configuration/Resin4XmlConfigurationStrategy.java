package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.util.Pair;
import org.intellij.j2ee.web.resin.ResinModel;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Resin4XmlConfigurationStrategy extends ResinXmlConfigurationStrategy {
  protected static final String RESIN_CONF = "resin4.xml";

  @NonNls
  private static final String HOST_DEFAULT_ELEMENT = "host-default";
  @NonNls
  private static final String WEB_APP_DEPLOY_ELEMENT = "web-app-deploy";
  @NonNls
  private static final String SERVER_MULTI_ELEMENT = "server-multi";

  @NonNls
  private static final String ROOT_DIR_ATTRIBUTE = "root-directory";
  @NonNls
  private static final String STARTUP_MODE_ATTRIBUTE = "startup-mode";
  @NonNls
  private static final String ID_PREFIX_ATTRIBUTE = "id-prefix";
  @NonNls
  private static final String ID_ATTRIBUTE = "id";

  @NonNls
  private static final String FIRST_SERVER_ID_SUFFIX = "0";

  @NonNls
  private static final String JVM_ARG = "jvm-arg";
  @NonNls
  private static final String JVM_ARG_SEPARATOR = "=";


  private String myServerId;

  Resin4XmlConfigurationStrategy(ResinInstallation resinInstallation) {
    super(resinInstallation);
  }

  @Override
  public void init(final ResinModel serverModel, Element element) throws ExecutionException {
    super.init(serverModel, element);

    Resin4ElementsProvider elementsProvider = (Resin4ElementsProvider)getElementsProvider();
    Namespace ns = elementsProvider.getNS();

    {
      Element hostDefaultElement = elementsProvider.getClusterElement().getChild(HOST_DEFAULT_ELEMENT, ns);
      if (hostDefaultElement == null) {
        hostDefaultElement = elementsProvider.getOrCreateChildElement(elementsProvider.getClusterDefaultElement(), HOST_DEFAULT_ELEMENT);
      }
      Element webAppDeployElement = elementsProvider.getOrCreateChildElement(hostDefaultElement, WEB_APP_DEPLOY_ELEMENT);

      webAppDeployElement.setAttribute(STARTUP_MODE_ATTRIBUTE, serverModel.getDeployMode());
    }

    Element serverElement = elementsProvider.getServerElement();
    if (serverElement != null) {
      myServerId = serverElement.getAttributeValue(ID_ATTRIBUTE);
    }
    else {
      if (elementsProvider.getServerDefaultElement() == null) {
        Element serverMultiElement = elementsProvider.getClusterElement().getChild(SERVER_MULTI_ELEMENT, ns);
        if (serverMultiElement != null) {
          myServerId = serverMultiElement.getAttributeValue(ID_PREFIX_ATTRIBUTE) + FIRST_SERVER_ID_SUFFIX;
        }
        else {
          throw new ExecutionException("Can't find neither 'server' nor 'server-default' nor 'server-multi' element");
        }
      }
    }

    Map<String, Element> jvmArgName2Element = new HashMap<>();
    List jvmArgElements = elementsProvider.getParamParentElement().getChildren(JVM_ARG, ns);
    for (Object jvmArg : jvmArgElements) {
      if (!(jvmArg instanceof Element jvmArgElement)) {
        continue;
      }
      String jvmArgText = jvmArgElement.getText();
      String[] jvmArgNameValue = jvmArgText.split(JVM_ARG_SEPARATOR, 2);
      jvmArgName2Element.put(jvmArgNameValue[0], jvmArgElement);
    }

    final List<ArgNameValue> jmxJvmArgs = new ArrayList<>();

    jmxJvmArgs.add(new ArgNameValue("-Dcom.sun.management.jmxremote.port", String.valueOf(serverModel.getJmxPort())));
    jmxJvmArgs.add(new ArgNameValue("-Dcom.sun.management.jmxremote.ssl", "false"));

    final File accessFile = serverModel.getAccessFile();
    final File passwordFile = serverModel.getPasswordFile();

    if (accessFile == null || passwordFile == null) {
      jmxJvmArgs.add(new ArgNameValue("-Dcom.sun.management.jmxremote.authenticate", "false"));
    }
    else {
      try {
        jmxJvmArgs.add(new ArgNameValue("-Dcom.sun.management.jmxremote.password.file", passwordFile.getCanonicalPath()));
        jmxJvmArgs.add(new ArgNameValue("-Dcom.sun.management.jmxremote.access.file", accessFile.getCanonicalPath()));
      }
      catch (IOException e) {
        throw new ExecutionException(e);
      }
    }

    for (ArgNameValue jmxJvmArg : jmxJvmArgs) {
      Element jvmArgElement = jvmArgName2Element.get(jmxJvmArg.getFirst());
      if (jvmArgElement == null) {
        jvmArgElement = new Element(JVM_ARG, ns);
        elementsProvider.getParamParentElement().addContent(jvmArgElement);
      }

      jvmArgElement.setText(jmxJvmArg.getFirst() + JVM_ARG_SEPARATOR + jmxJvmArg.getSecond());
    }
  }

  @Override
  public String getServerId() {
    return myServerId;
  }

  @Override
  protected String getExpandDirAttr() {
    return ROOT_DIR_ATTRIBUTE;
  }

  @Override
  public InputStream getDefaultResinConfContent() {
    return this.getClass().getResourceAsStream(RESIN_CONF);
  }

  private static class ArgNameValue extends Pair<String, String> {

    ArgNameValue(@NonNls String first, @NonNls String second) {
      super(first, second);
    }
  }

  @Override
  protected ElementsProvider createElementsProvider() {
    return new Resin4ElementsProvider(getElement());
  }

  private class Resin4ElementsProvider extends Resin31ElementsProvider {

    Resin4ElementsProvider(Element element) {
      super(element);
    }

    public Element getDirectClusterDefaultElement() {
      return getRootElement().getChild(CLUSTER_DEFAULT_ELEMENT, getNS());
    }

    @Override
    protected Element doGetClusterDefaultElement() {
      Element result = getDirectClusterDefaultElement();
      if (result != null) {
        return result;
      }
      for (ResinConfigImport configImport : getImports()) {
        Element element = configImport.getImportDoc();
        if (element != null) {
          Resin4ElementsProvider importElementsProvider = new Resin4ElementsProvider(element);
          result = importElementsProvider.getDirectClusterDefaultElement();
          if (result != null) {
            if (getInstallation().getVersion().getParsed().compare(4, 0, 41) >= 0) {
              resolveImports(element, null);
            }
            configImport.copy();
            return result;
          }
        }
      }
      return super.doGetClusterDefaultElement();
    }
  }
}
