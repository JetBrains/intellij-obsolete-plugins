package com.intellij.gwt.maven;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtMavenSdkPaths;

import static com.intellij.gwt.maven.GwtCodehausFacetImporter.getSizeInMegabytes;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static java.util.Collections.singleton;

public final class GwtLtgtFacetImporter extends GwtFacetImporter {

  public GwtLtgtFacetImporter() {
    super("net.ltgt.gwt.maven", "gwt-maven-plugin");
  }

  @Override
  protected void setupGwtSdk(GwtFacet facet, MavenProject project) {
    GwtPath path = resolveGwtPath(project);
    if (path == null) return;

    final GwtFacetConfiguration configuration = facet.getConfiguration();
    configuration.setGwtSdkUrl(VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(path.path)));
    configuration.setGwtSdkType(path.isInstalled ? null : GwtMavenSdkPaths.TYPE_ID);
  }

  @Override
  protected void setupGwtCompilerOptions(GwtFacetConfiguration configuration, Module module, MavenProject project) {
    configuration.setCompilerParameters(getCompilerParameters(project));
    configuration.setOutputStyle(GwtJavaScriptOutputStyle.byId(findConfigValue(project, "style")));

    configureAdditionalCompilerVmParameters(configuration, project);

    String moduleName = findConfigValue(project, "moduleName");
    if (isNotEmpty(moduleName)) {
      setEnabledGwtModulesDumbAware(module, singleton(moduleName), configuration);
    }
  }

  private @NotNull String getCompilerParameters(MavenProject project) {
    ParametersList compilerParameters = new ParametersList();

    Element compilerArgs = getConfig(project, "compilerArgs");
    if (compilerArgs != null) {
      for (Element compilerArg : compilerArgs.getChildren("compilerArg")) {
        compilerParameters.add(compilerArg.getText());
      }
    }

    if ("true".equals(findConfigValue(project, "draftCompile"))) {
      compilerParameters.add("-draftCompile");
    }

    if ("true".equals(findConfigValue(project, "failOnError"))) {
      compilerParameters.add("-failOnError");
    }

    String logLevel = findConfigValue(project, "logLevel");
    if (isNotEmpty(logLevel)) {
      compilerParameters.add("-logLevel");
      compilerParameters.add(logLevel);
    }

    String localWorkers = findConfigValue(project, "localWorkers");
    if (isNotEmpty(localWorkers)) {
      compilerParameters.add("-localWorkers");
      compilerParameters.add(localWorkers);
    }

    String optimize = findConfigValue(project, "optimize");
    if (isNotEmpty(optimize)) {
      compilerParameters.add("-optimize");
      compilerParameters.add(optimize);
    }

    String sourceLevel = findConfigValue(project, "sourceLevel");
    if (isNotEmpty(sourceLevel)) {
      compilerParameters.add("-sourceLevel");
      compilerParameters.add(sourceLevel);
    }

    return compilerParameters.getParametersString();
  }

  private void configureAdditionalCompilerVmParameters(GwtFacetConfiguration configuration, MavenProject project) {
    ParametersList additionalVmParameters = new ParametersList();
    Element jvmArgs = getConfig(project, "jvmArgs");
    if (jvmArgs != null) {
      for (Element jvmArg : jvmArgs.getChildren("jvmArg")) {
        @NonNls String jvmArgText = jvmArg.getText();
        if (jvmArgText.startsWith("-Xmx")) {
          configuration.setCompilerMaxHeapSize(getSizeInMegabytes(jvmArgText));
        }
        else {
          additionalVmParameters.add(jvmArgText);
        }
      }
    }

    Element systemProperties = getConfig(project, "systemProperties");
    if (systemProperties != null) {
      for (Element systemProperty : systemProperties.getChildren()) {
        additionalVmParameters.add("-D" + systemProperty.getName() + "=" + systemProperty.getText());
      }
    }

    configuration.setAdditionalCompilerVMParameters(additionalVmParameters.getParametersString());
  }
}
