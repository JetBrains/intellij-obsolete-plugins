package org.intellij.j2ee.web.resin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.javaee.appServers.run.configuration.JavaCommandLineStartupPolicy;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.jmxremote.JmxRemotePrepareResult;
import com.intellij.javaee.jmxremote.JmxRemoteUtil;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.util.PathsList;
import org.intellij.j2ee.web.resin.resin.ResinConfiguration;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.intellij.j2ee.web.resin.resin.version.ResinVersion;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;

public class ResinStartupPolicy implements JavaCommandLineStartupPolicy {

  private static final Logger LOG = Logger.getInstance(ResinStartupPolicy.class);
  @NonNls
  private static final String RESIN_RUN_PROP_FILE = "ResinRun.properties";
  @NonNls
  private static final String DEBUG_VM_PARAMS_PROP = "resin.debug.vm.param";
  @NonNls
  private static final String JMX_VM_PARAMS_PROP = "resin.jmx.vm.param";
  @NonNls
  private static final String RESINHOME_VM_PARAMS_PROP = "resin.home.vm.param";
  @NonNls
  private static final String JAVA_LIB_PATH_VM_PARAMS_PROP = "java.lib.path.vm.param";
  @NonNls
  private static final String SERVER_ID_VM_PARAMS_PROP = "server.id.vm.param";
  @NonNls
  private static final String COMMAND_LINE_CONF_ARG_PROP = "resin.command.line.conf.arg.name";
  @NonNls
  private static final String COMMAND_LINE_SERVER_ID_ARG_PROP = "resin.command.line.server.id.arg.name";
  @NonNls
  private static final String RESIN_VERSIONS_INVALID_PATHS_PROP = "resin.versions.not.allow.white.spaces";
  @NonNls
  private static final String RESIN_PROPERTIES_FILE_PATH = "/conf/resin.properties";
  @NonNls
  private static final String RESIN_JVM_ARGS_PROP = "jvm_args";

  private Properties resinRunProps = null;

  @Override
  public JavaParameters createCommandLine(final CommonModel commonModel) throws ExecutionException {
    final ResinModel resinModel = (ResinModel)commonModel.getServerModel();
    final JmxRemotePrepareResult prepareResult;

    if (resinModel.hasJmxStrategy()) {
      prepareResult = JmxRemoteUtil.prepare(resinModel);

      JmxRemoteUtil.apply(resinModel, prepareResult);
    }
    else {
      prepareResult = null;
    }

    if (prepareResult == null) {
      resinModel.setAccessFile(null);
      resinModel.setPasswordFile(null);
    }
    else {
      resinModel.setAccessFile(prepareResult.getAccessFile());
      resinModel.setPasswordFile(prepareResult.getPasswordFile());
    }

    final ResinConfiguration resinConfiguration = resinModel.getOrCreateResinConfiguration(true);

    ResinInstallation installation = resinConfiguration.getInstallation();

    final String homePath = FileUtil.toSystemDependentName(installation.getResinHome().getPath());

    final JavaParameters parameters = new JavaParameters();

    String charset = resinModel.getCharset();
    if (charset != null && !charset.isEmpty()) {
      parameters.setCharset(Charset.forName(resinModel.getCharset()));
    }

    if (resinConfiguration.getInstallation().getVersion().allowXdebug()) {
      loadResinRunProp(DEBUG_VM_PARAMS_PROP, parameters);
    }

    ResinVersion resinVersion = resinConfiguration.getInstallation().getVersion();
    parameters.setWorkingDirectory(homePath);
    parameters.setMainClass(resinVersion.getStartupClass());

    if (resinModel.hasJmxStrategy()) {
      loadResinRunProp(JMX_VM_PARAMS_PROP, parameters, String.valueOf(resinModel.getJmxPort()));

      JmxRemoteUtil.apply(parameters.getVMParametersList(), prepareResult);
    }

    loadResinRunProp(RESINHOME_VM_PARAMS_PROP, parameters, homePath);

    if (homePath.indexOf(' ') != -1 && !allowsRunWithWhiteSpace(resinVersion)) {
      throw new ExecutionException(ResinBundle.message("resin.run.error.invalid.path", homePath));
    }

    loadResinRunProp(JAVA_LIB_PATH_VM_PARAMS_PROP, parameters, homePath);

    ParametersList parametersList = parameters.getProgramParametersList();
    parametersList.add(getResinRunProperty(COMMAND_LINE_CONF_ARG_PROP)[0],
                       resinConfiguration.getConfigFile().getAbsolutePath());
    String serverId = resinConfiguration.getServerId();
    if (!StringUtil.isEmpty(serverId)) {
      parametersList.add(getResinRunProperty(COMMAND_LINE_SERVER_ID_ARG_PROP)[0], serverId);
      loadResinRunProp(SERVER_ID_VM_PARAMS_PROP, parameters, serverId);
    }
    String additionalParameters = resinModel.getAdditionalParameters();
    if (additionalParameters != null && !additionalParameters.isEmpty()) {
      parametersList.addParametersString(additionalParameters);
    }
    loadResinConfProperties(homePath, parameters);

    final PathsList classpath = parameters.getClassPath();

    //Include application server libraries
    VirtualFile[] files = commonModel.getApplicationServer().getLibrary().getFiles(OrderRootType.CLASSES);
    for (VirtualFile file : files) {
      classpath.add(file.getPresentableUrl());
    }

    File[] allJars = resinConfiguration.getInstallation().getLibFiles(true);
    for (File jar : allJars) {
      String s = jar.getAbsolutePath();
      if (!classpath.getPathList().contains(s)) {
        classpath.add(s);
      }
    }

    if (resinModel.isAutoBuildClassPath()) {
      Collection<VirtualFile> outputAndLibs = new ArrayList<>();
      for (DeploymentModel model : commonModel.getDeploymentModels()) {
        final Artifact artifact = model.getArtifact();
        if (artifact == null) {
          continue;
        }
        final Collection<WebFacet> webFacets =
          JavaeeArtifactUtil.getInstance().getFacetsIncludedInArtifact(commonModel.getProject(), artifact, WebFacet.ID);
        for (WebFacet webFacet : webFacets) {
          ModuleRootManager mrm = ModuleRootManager.getInstance(webFacet.getModule());
          final VirtualFile[] roots = mrm.orderEntries().withoutSdk().recursively().exportedOnly().getClassesRoots();
          Collections.addAll(outputAndLibs, roots);
        }
      }

      for (VirtualFile vfile : outputAndLibs) {
        classpath.add(vfile);
      }
    }

    return parameters;
  }

  /**
   * Checks if this resin version can run within a resin home path with white spaces
   *
   * @param resinVersion the resin version
   * @return true if the version can run within white space path. otherwise false
   * @throws ExecutionException if the property file is invalid (no invalid version prop specify)
   */
  private boolean allowsRunWithWhiteSpace(ResinVersion resinVersion) throws ExecutionException {
    String[] value = getResinRunProperty(RESIN_VERSIONS_INVALID_PATHS_PROP);
    List invalids = Arrays.asList(value);

    String verNumber = resinVersion.getVersionNumber();
    if (invalids.contains(verNumber)) {
      return false;
    }

    //Fallback into wildcard version
    String[] tocheck = verNumber.split("\\.");
    for (String actual : tocheck) {
      if (invalids.contains(actual + ".x")) {
        return false;
      }
    }


    return true;
  }

  /**
   * Loads a resin run property, and add it to the Java VM parameter list
   *
   * @param prop         the property to load
   * @param parameters   the java parameter list
   * @param substitution property substituion parameters
   * @throws ExecutionException if the property doesn't exist
   */
  private void loadResinRunProp(String prop, JavaParameters parameters, Object... substitution) throws ExecutionException {
    String[] values = getResinRunProperty(prop, substitution);
    for (String value : values) {
      parameters.getVMParametersList().add(value);
    }
  }

  /**
   * Gets a resin run property
   *
   * @param prop         the property to get
   * @param substitution property substituion parameters
   * @return property value
   * @throws ExecutionException if the property doesn't exist
   */
  private String[] getResinRunProperty(String prop, Object... substitution) throws ExecutionException {
    loadResinRunProperties();

    String value = resinRunProps.getProperty(prop);
    if (value == null) {
      throw new ExecutionException(ResinBundle.message("resin.run.property.missing", prop));
    }

    String[] res = value.split(" ");
    if (substitution != null) {
      for (int i = 0; i < res.length; i++) {
        res[i] = MessageFormat.format(res[i], substitution);
      }
    }

    return res;
  }

  /**
   * Loads resin run properties
   *
   * @throws ExecutionException if any exception occurs during properties read
   */
  private void loadResinRunProperties() throws ExecutionException {
    if (resinRunProps != null) {
      return;
    }
    resinRunProps = new Properties();
    try {
      resinRunProps.load(this.getClass().getResourceAsStream(RESIN_RUN_PROP_FILE));
    }
    catch (IOException e) {
      throw new ExecutionException(ResinBundle.message("resin.run.startup.no.prop"));
    }
  }

  private static void loadResinConfProperties(String homePath, JavaParameters parameters) {
    File propertiesFile = new File(FileUtil.toSystemDependentName(homePath + RESIN_PROPERTIES_FILE_PATH));
    if (!propertiesFile.exists()) {
      return;
    }
    Properties props = new Properties();
    try {
      props.load(new FileReader(propertiesFile));
    }
    catch (IOException e) {
      LOG.info(e);
    }
    parameters.getVMParametersList().addParametersString(props.getProperty(RESIN_JVM_ARGS_PROP));
  }
}
