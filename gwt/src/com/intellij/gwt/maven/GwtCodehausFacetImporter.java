package com.intellij.gwt.maven;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.execution.ParametersListUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtMavenSdkPaths;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GwtCodehausFacetImporter extends GwtFacetImporter {
  private static final Logger LOG = Logger.getInstance(GwtCodehausFacetImporter.class);

  public GwtCodehausFacetImporter() {
    this("org.codehaus.mojo", "gwt-maven-plugin");
  }

  private GwtCodehausFacetImporter(@NonNls String pluginGroupId, @NonNls String pluginArtifactId) {
    super(pluginGroupId, pluginArtifactId);
  }

  @Override
  public void resolve(Project project, MavenProject mavenProject,
                      MavenEmbedderWrapper embedder) throws MavenProcessCanceledException {
    super.resolve(project, mavenProject, embedder);
    addSourcesDependencies(mavenProject, embedder, getConfig(mavenProject, "compileSourcesArtifacts"));
  }

  static void addSourcesDependencies(MavenProject mavenProject, MavenEmbedderWrapper embedder, Element compileSourcesArtifacts) throws MavenProcessCanceledException {
    List<MavenRemoteRepository> repos = mavenProject.getRemoteRepositories();
    List<Element> artifactTags = JDOMUtil.getChildren(compileSourcesArtifacts);
    LinkedHashSet<MavenArtifact> toAdd = new LinkedHashSet<>();
    for (Element artifactTag : artifactTags) {
      List<String> parts = StringUtil.split(artifactTag.getTextTrim(), ":");
      if (parts.size() < 2) continue;
      String groupId = parts.get(0);
      String artifactId = parts.get(1);

      for (MavenArtifact artifact : mavenProject.getDependencies()) {
        if (artifact.getMavenId().equals(groupId, artifactId)) {
          MavenArtifactInfo artifactInfo =
            new MavenArtifactInfo(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "jar", "sources");
          toAdd.add(embedder.resolve(artifactInfo, repos));
        }
      }
    }
    mavenProject.addDependencies(toAdd);
  }

  @Override
  protected void setupGwtCompilerOptions(final GwtFacetConfiguration configuration, final Module module, MavenProject project) {
    String extraJvmArgs = findConfigValue(project, "extraJvmArgs");
    List<String> additionalCompilerVmParameters = new ArrayList<>();
    if (extraJvmArgs != null) {
      List<String> jvmArgs = ParametersListUtil.parse(extraJvmArgs);
      for (@NonNls String arg : jvmArgs) {
        if (arg.startsWith("-Xmx")) {
          configuration.setCompilerMaxHeapSize(getSizeInMegabytes(arg));
        }
        else {
          additionalCompilerVmParameters.add(arg);
        }
      }
    }
    GwtVersionImpl version = (GwtVersionImpl)configuration.getSdk().getVersion();
    for (GwtCodehausParameter property : GwtCodehausParameter.PROPERTIES) {
      property.append(version, additionalCompilerVmParameters, findConfigValue(project, property.getTagName()));
    }
    configuration.setAdditionalCompilerVMParameters(ParametersListUtil.join(additionalCompilerVmParameters));

    GwtJavaScriptOutputStyle outputStyle = GwtJavaScriptOutputStyle.byId(findConfigValue(project, "style"));
    if (outputStyle != null) {
      configuration.setOutputStyle(outputStyle);
    }
    List<String> compilerParameters = new ArrayList<>();
    for (GwtCodehausParameter parameter : GwtCodehausParameter.PARAMETERS) {
      parameter.append(version, compilerParameters, findConfigValue(project, parameter.getTagName()));
    }
    configuration.setCompilerParameters(StringUtil.join(compilerParameters, " "));

    final Set<String> enabledModules = new HashSet<>();
    String singleModuleName = findConfigValue(project, "module");
    if (singleModuleName != null) {
      enabledModules.add(singleModuleName);
    }
    else {
      List<Element> moduleTags = JDOMUtil.getChildren(getConfig(project, "modules"), "module");
      for (Element tag : moduleTags) {
        enabledModules.add(tag.getTextTrim());
      }
    }
    setEnabledGwtModulesDumbAware(module, enabledModules, configuration);
  }

  public static int getSizeInMegabytes(String xmxArg) {
    @NonNls String sizeString = StringUtil.trimStart(xmxArg, "-Xmx");
    if (!sizeString.isEmpty()) {
      try {
        int size = Integer.parseInt(sizeString.substring(0, sizeString.length() - 1));
        if (StringUtil.endsWithIgnoreCase(sizeString, "g")) {
          return size * 1024;
        }
        return size;
      }
      catch (NumberFormatException e) {
        LOG.info(e);
      }
    }
    return 0;
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
  protected @Nullable GwtPath resolveGwtPath(MavenProject project) {
    String sdkPath = findGwtPath(project);
    if (sdkPath != null) return new GwtPath(sdkPath, null, true);

    String version = findConfigValue(project, "gwtVersion");
    if (version != null) {
      return new GwtPath(getGwtDevPath(project, version), version, false);
    }

    return super.resolveGwtPath(project);
  }
}
