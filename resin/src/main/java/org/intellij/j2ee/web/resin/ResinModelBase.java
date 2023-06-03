package org.intellij.j2ee.web.resin;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerUrlMapping;
import com.intellij.javaee.appServers.run.configuration.ServerModel;
import com.intellij.javaee.appServers.run.execution.DefaultOutputProcessor;
import com.intellij.javaee.appServers.run.execution.OutputProcessor;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.intellij.j2ee.web.resin.resin.ResinPersistentDataHelper;
import org.intellij.j2ee.web.resin.resin.configuration.JmxConfigurationStrategy;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class ResinModelBase<D extends ResinModelDataBase> implements ServerModel {

  private D myData = createResinModelData();

  private CommonModel myCommonModel;

  @Override
  public final int getDefaultPort() {
    return ResinUtil.DEFAULT_PORT;
  }

  @Override
  public final void setCommonModel(CommonModel commonModel) {
    myCommonModel = commonModel;
  }

  public final CommonModel getCommonModel() {
    return myCommonModel;
  }

  @Override
  public final J2EEServerInstance createServerInstance() {
    return new ResinServerInstance(myCommonModel);
  }

  @Override
  public final DeploymentProvider getDeploymentProvider() {
    return new ResinDeploymentProvider();
  }

  @Override
  @NotNull
  public final String getDefaultUrlForBrowser() {
    ApplicationServerUrlMapping urlMapping = (ApplicationServerUrlMapping)myCommonModel.getIntegration().getDeployedFileUrlProvider();
    return urlMapping.getDefaultUrlForServerConfig(myCommonModel);
  }

  @Override
  public final OutputProcessor createOutputProcessor(ProcessHandler processHandler, J2EEServerInstance j2EEServerInstance) {
    return new DefaultOutputProcessor(processHandler);
  }

  public final Project getProject() {
    return myCommonModel.getProject();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  protected final D getData() {
    return myData;
  }

  @Override
  public int getLocalPort() {
    return getData().getPort();
  }

  public int getPort() {
    return getData().getPort();
  }

  public void setPort(int port) {
    getData().setPort(port);
  }

  public int getJmxPort() {
    return getData().getJmxPort();
  }

  public void setJmxPort(int jmxPort) {
    getData().setJmxPort(jmxPort);
  }

  public String getCharset() {
    String charset = getData().getCharset();
    return charset == null ? "" : charset.trim();
  }

  public void setCharset(String charset) {
    getData().setCharset(charset);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    Set<String> contexts = new HashSet<>();
    for (DeploymentModel deploymentModel : getCommonModel().getDeploymentModels()) {
      final ResinModuleDeploymentModel model = (ResinModuleDeploymentModel)deploymentModel;
      String contextPath = model.getContextPath();
      if (!model.isDefaultContextPath() && !contexts.add(contextPath)) {
        throw new RuntimeConfigurationError(ResinBundle.message("error.duplicate.context.path.text", contextPath));
      }
    }
  }

  @Override
  public final void readExternal(Element element) throws InvalidDataException {
    D data = createResinModelData();
    //DefaultJDOMExternalizer.readExternal(this, element);
    XmlSerializer.deserializeInto(data, element);
    myData = data;
  }

  @Override
  public final void writeExternal(Element element) throws WriteExternalException {
    //DefaultJDOMExternalizer.writeExternal(this, element);
    XmlSerializer.serializeInto(myData, element, new SkipDefaultValuesSerializationFilters());
  }

  public ResinPersistentDataHelper getHelper() {
    return new ResinPersistentDataHelper(getCommonModel().getApplicationServer());
  }

  @Nullable
  public ResinInstallation getInstallation() {
    return getHelper().getInstallation();
  }

  @Nullable
  public JmxConfigurationStrategy getJmxStrategy() {
    return getHelper().getJmxStrategy();
  }

  public boolean hasJmxStrategy() {
    return getHelper().hasJmxStrategy();
  }

  protected abstract D createResinModelData();

  public abstract boolean transferFile(File webAppFile);

  public abstract boolean deleteFile(File webAppFile);

  @Nullable
  public abstract SettingsEditor<DeploymentModel> createAdditionalDeploymentSettingsEditor(CommonModel commonModel,
                                                                                           DeploymentSource source);

  @Nullable
  public String getJmxUsername() {
    return null;
  }

  @Nullable
  public String getJmxPassword() {
    return null;
  }
}
