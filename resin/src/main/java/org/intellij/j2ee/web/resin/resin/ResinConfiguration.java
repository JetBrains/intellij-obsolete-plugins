package org.intellij.j2ee.web.resin.resin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.intellij.j2ee.web.resin.ResinDeploymentProvider;
import org.intellij.j2ee.web.resin.ResinModel;
import org.intellij.j2ee.web.resin.resin.configuration.ResinConfigurationStrategy;
import org.intellij.j2ee.web.resin.resin.configuration.ResinGeneratedConfig;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ResinConfiguration {

  private static final Logger LOG = Logger.getInstance(ResinConfiguration.class);

  @NonNls
  private static final String JAVAC_ELEMENT = "javac";
  @NonNls
  private static final String ARGS_ATTRIBUTE = "args";
  @NonNls
  private static final String COMPILER_ATTRIBUTE = "compiler";
  @NonNls
  private static final String COMPILER_ATTRIBUTE_VALUE = "internal";
  @NonNls
  private static final String ARGS_ATTRIBUTE_VALUE = "-source 1.5";
  @NonNls
  private static final String ARGS_ATTRIBUTE_VALUE_PREFIX = "-g";

  private final ResinInstallation myInstallation;
  private final ResinGeneratedConfig myGeneratedConfig;
  private final File mySourceConfig;
  private final ResinConfigurationStrategy myStrategy;

  public ResinConfiguration(ResinModel serverModel) throws ExecutionException {
    ResinPersistentDataHelper helper = serverModel.getHelper();

    myInstallation = helper.getInstallationOrError();
    LOG.assertTrue(myInstallation != null);

    try {
      mySourceConfig = serverModel.findConfFile();
    }
    catch (RuntimeConfigurationException e) {
      throw new ExecutionException(e.getMessage());
    }

    myStrategy = helper.getStrategy();
    LOG.assertTrue(myStrategy != null);

    if (serverModel.isReadOnlyConfiguration()) {
      myGeneratedConfig = null;
    }
    else {
      try {
        Element document;
        if (mySourceConfig.length() == 0) {
          InputStream is = myStrategy.getDefaultResinConfContent();
          if (is == null) {
            throw new ExecutionException(ResinBundle.message("run.resin.conf.doesnt.exist"));
          }
          document = JDOMUtil.load(is);
        }
        else {
          document = JDOMUtil.load(mySourceConfig);
        }
        myGeneratedConfig = new ResinGeneratedConfig(document, "resin");
        patchConfigToMakeDebuggerWork(document);
        myStrategy.init(serverModel, document);

        myStrategy.setPort(serverModel.getPort());
      }
      catch (JDOMException | IOException e) {
        throw new ExecutionException(ResinBundle.message("run.resin.conf.load.error"), e);
      }

      for (DeploymentModel model : serverModel.getCommonModel().getDeploymentModels()) {
        if (model.getDeploymentMethod() == ResinDeploymentProvider.CONF_DEPLOYMENT_METHOD) {
          WebApp webApp = ResinDeploymentProvider.getWebApp(model);
          if (webApp != null) {
            deploy(webApp);
          }
        }
      }
      save();
    }
  }

  public String getServerId() {
    return myStrategy.getServerId();
  }

  private static void patchConfigToMakeDebuggerWork(@NotNull Element element) {
    Element javac = element.getChild(JAVAC_ELEMENT, element.getNamespace());
    if (javac == null) {
      javac = new Element(JAVAC_ELEMENT, element.getNamespace());
      javac.setAttribute(COMPILER_ATTRIBUTE, COMPILER_ATTRIBUTE_VALUE);
      javac.setAttribute(ARGS_ATTRIBUTE, ARGS_ATTRIBUTE_VALUE);
      element.addContent(javac);
    }
    final Attribute args = javac.getAttribute(ARGS_ATTRIBUTE);
    if (!args.getValue().contains(ARGS_ATTRIBUTE_VALUE_PREFIX)) {
      args.setValue(ARGS_ATTRIBUTE_VALUE_PREFIX + " " + args.getValue());
    }
  }

  public ResinInstallation getInstallation() {
    return myInstallation;
  }

  private boolean isWritable() {
    return myGeneratedConfig != null;
  }

  public File getConfigFile() {
    return isWritable() ? myGeneratedConfig.getFile() : mySourceConfig;
  }

  public void deploy(final WebApp webApp) throws ExecutionException {
    if (!isWritable()) {
      return;
    }
    myStrategy.deploy(webApp);
    save();
  }

  public boolean undeploy(WebApp webApp) throws ExecutionException {
    if (!isWritable()) {
      return false;
    }
    boolean result = myStrategy.undeploy(webApp);
    save();
    return result;
  }

  private void save() throws ExecutionException {
    myGeneratedConfig.save();
    myStrategy.save();
  }
}
