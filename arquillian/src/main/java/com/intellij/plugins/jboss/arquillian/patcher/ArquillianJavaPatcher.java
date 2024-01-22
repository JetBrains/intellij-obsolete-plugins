package com.intellij.plugins.jboss.arquillian.patcher;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.WrappingRunConfiguration;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.jboss.arquillian.MavenManager;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianExistLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianMavenLibraryState;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianRunConfigurationCoordinator;
import com.intellij.plugins.jboss.arquillian.runConfiguration.ArquillianTestFrameworkRunConfiguration;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.JBIterable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ArquillianJavaPatcher extends JavaProgramPatcher {
  ArquillianJavaPatcher() {
  }

  @Override
  public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
    while (configuration instanceof WrappingRunConfiguration) {
      configuration = ((WrappingRunConfiguration<?>)configuration).getPeer();
    }
    if (!(configuration instanceof ArquillianTestFrameworkRunConfiguration frameworkRunConfiguration)) {
      return;
    }
    ArquillianRunConfigurationCoordinator coordinator = new ArquillianRunConfigurationCoordinator(frameworkRunConfiguration.getProject());
    ArquillianContainerState containerState = coordinator.getContainerState(frameworkRunConfiguration.getRunConfiguration());
    if (containerState == null) {
      return;
    }
    List<String> jars = JBIterable.from(containerState.libraries).flatten(libraryState -> libraryState.accept(new ArquillianLibraryState.Visitor<Iterable<String>>() {
      @Override
      public Iterable<String> visitMavenLibrary(ArquillianMavenLibraryState libraryState) {
        return MavenManager.getInstance().getMavenArtifactJars(
          libraryState.groupId,
          libraryState.artifactId,
          libraryState.version);
      }

      @Override
      public Iterable<String> visitExistLibrary(ArquillianExistLibraryState state) {
        final Library library = state.findLibrary(frameworkRunConfiguration.getProject());
        if (library == null) {
          return Collections.emptyList();
        }
        return JBIterable.of(OrderRootType.CLASSES, OrderRootType.SOURCES, OrderRootType.DOCUMENTATION)
          .flatten(type -> JBIterable.of(library.getFiles(type)).transform(file -> PathUtil.toPresentableUrl(file.getUrl())).toList());
      }
    })).toList();

    for (String jar : jars) {
      javaParameters.getClassPath().add(jar);
    }

    javaParameters.getVMParametersList().addParametersString(containerState.getJvmParameters());

    String containerQualifier = StringUtil.toLowerCase(executor.getActionName()).equals("debug")
                                ? containerState.configurationSpecificState.debugContainerQualifier.trim()
                                : containerState.configurationSpecificState.runContainerQualifier.trim();

    if (StringUtil.isNotEmpty(containerQualifier)) {
      javaParameters.getVMParametersList().addParametersString("-Darquillian.launch=" + containerQualifier);
    }

    Map<String, String> envVariables = containerState.getEnvVariables();
    for (String name : envVariables.keySet()) {
      javaParameters.addEnv(name, envVariables.get(name));
    }
  }
}
