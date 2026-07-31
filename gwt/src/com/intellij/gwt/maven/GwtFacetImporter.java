package com.intellij.gwt.maven;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetArtifactValidator;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.packaging.GwtCompileOutputRelativePathSuggester;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.gwt.sdk.GwtDependenciesStorage;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.project.PackagingModifiableModel;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.platform.backend.observation.TrackingUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.ZipUtil;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.utils.MavenActivityKey;
import org.jetbrains.idea.maven.utils.MavenLog;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtMavenSdkPaths;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jps.gwt.model.GwtSdkPaths.isGwtMavenGroupId;

public abstract class GwtFacetImporter extends FacetImporter<GwtFacet, GwtFacetConfiguration, GwtFacetType> {

  protected GwtFacetImporter(@NonNls String pluginGroupId, @NonNls String pluginArtifactId) {
    super(pluginGroupId, pluginArtifactId, GwtFacetType.getInstance());
  }

  @Override
  public boolean isApplicable(MavenProject mavenProject) {
    return mavenProject.findPlugin(myPluginGroupID, myPluginArtifactID, true) != null;
  }

  public void resolve(Project project, MavenProject mavenProject,
                      MavenEmbedderWrapper embedder) throws MavenProcessCanceledException {
    GwtPath path = resolveGwtPath(mavenProject);
    if (path == null || path.isInstalled) return;

    boolean platformDependent = VersionComparatorUtil.compare(path.version, "2.0") < 0;
    String platform = platformDependent ? GwtMavenSdkPaths.getPlatformId() : null;

    List<MavenRemoteRepository> repos = mavenProject.getRemoteRepositories();

    storeGwtDependencies(GwtSdkPaths.GWT_DEV_ARTIFACT_ID, path.version, platform, mavenProject, embedder, repos, project);
    storeGwtDependencies(GwtSdkPaths.GWT_USER_ARTIFACT_ID, path.version, null, mavenProject, embedder, repos, project);

    if (platformDependent) {
      MavenArtifact libs = embedder.resolve(new MavenArtifactInfo(GwtSdkPaths.OLD_GROUP_ID, GwtSdkPaths.GWT_DEV_ARTIFACT_ID,
                                                                  path.version, "zip", platform + "-libs"), repos);
      File file = libs.getFile();
      if (file.exists()) {
        try {
          ZipUtil.extract(file, file.getParentFile(), null, false);
        }
        catch (IOException e) {
          MavenLog.LOG.warn("cannot unpack gwt libraries" + e);
          // todo
        }
      }
    }
  }

  private static void storeGwtDependencies(String artifactId, String gwtVersion, String classifier,
                                           MavenProject mavenProject, MavenEmbedderWrapper embedder, List<MavenRemoteRepository> repos,
                                           Project project) throws MavenProcessCanceledException {
    MavenArtifactNode dependencyInProject =
      ContainerUtil.find(mavenProject.getDependencyTree(), node -> node.getArtifact().getArtifactId().equals(artifactId)
                                                                   && isGwtMavenGroupId(node.getArtifact().getGroupId()));
    Collection<MavenArtifact> artifacts;
    if (dependencyInProject != null) {
      artifacts = resolveDependenciesRecursive(dependencyInProject.getDependencies(), new LinkedHashSet<>());
    }
    else {
      artifacts = GwtFacetImporterUtil.resolveArtifactTransitively(artifactId, gwtVersion, classifier, embedder, repos);
    }

    List<String> paths = ContainerUtil.map(artifacts, dep -> dep.getFile().getAbsolutePath());

    boolean isGoogle = VersionComparatorUtil.compare(gwtVersion, "2.10") < 0;

    if (isGoogle) {
      GwtDependenciesStorage.getInstance(project).storeDependenciesPaths(GwtSdkPaths.OLD_GROUP_ID, artifactId, gwtVersion, paths);
    }
    else {
      GwtDependenciesStorage.getInstance(project).storeDependenciesPaths(GwtSdkPaths.NEW_GROUP_ID, artifactId, gwtVersion, paths);
    }
  }

  private static Set<MavenArtifact> resolveDependenciesRecursive(List<MavenArtifactNode> dependencies, Set<MavenArtifact> gwtDevDependencies) {
    for (MavenArtifactNode node : dependencies) {
      MavenArtifact artifact = node.getArtifact();
      if (!gwtDevDependencies.contains(artifact)) {
        gwtDevDependencies.add(artifact);
        resolveDependenciesRecursive(node.getDependencies(), gwtDevDependencies);
      }
    }
    return gwtDevDependencies;
  }

  @Override
  protected void reimportFacet(@NotNull IdeModifiableModelsProvider modelsProvider,
                               @NotNull Module module,
                               @NotNull MavenRootModelAdapter rootModel,
                               @NotNull GwtFacet facet,
                               @NotNull MavenProjectsTree mavenTree,
                               @NotNull MavenProject project,
                               @NotNull MavenProjectChanges changes, @NotNull Map<MavenProject, String> mavenProjectToModuleName,
                               @NotNull List<MavenProjectsProcessorTask> postTasks) {
    MavenLog.LOG.debug("GwtFacetImporter.reimportFacet started");
    TrackingUtil.trackActivity(module.getProject(), MavenActivityKey.INSTANCE, () -> {
      IdeModifiableModelsProviderImpl modelsProvider1 = new IdeModifiableModelsProviderImpl(module.getProject());
      DumbService.getInstance(module.getProject()).smartInvokeLater(() -> {
        setupGwtSdk(facet, project);
        setupGwtCompilerOptions(facet.getConfiguration(), module, project);

        WebFacet webFacet = findFacet(modelsProvider1.getModifiableFacetModel(module), WebFacetType.getInstance(), "Web");
        if (webFacet != null) {
          facet.getConfiguration().setWebFacetName(webFacet.getName());
        }

        addGwtCompilerOutputToArtifact(modelsProvider1, module, facet, project, webFacet);
        WriteAction.run(() -> {
          modelsProvider1.commit();
        });
        MavenLog.LOG.debug("GwtFacetImporter.reimportFacet finished");
      });
    });
  }

  protected void setupGwtCompilerOptions(GwtFacetConfiguration configuration, Module module, MavenProject project) {
  }

  protected static void setEnabledGwtModulesDumbAware(@NotNull Module module, @NotNull Set<String> enabledModules, GwtFacetConfiguration configuration) {
    if (enabledModules.isEmpty()) {
      return;
    }
    DumbService.getInstance(module.getProject()).smartInvokeLater(() -> {
      GwtFacet facet = GwtFacet.getInstance(module);
      if (facet != null) {
        setEnabledGwtModules(module, enabledModules, facet.getConfiguration());
      } else {
        setEnabledGwtModules(module, enabledModules, configuration);
      }
    });
  }

  private static void setEnabledGwtModules(@NotNull Module module, @NotNull Set<String> enabledModules, GwtFacetConfiguration configuration) {
    List<GwtModule> modules = GwtModulesManager.getInstance(module.getProject()).getGwtModules(module, true);
    for (GwtModule gwtModule : modules) {
      String moduleName = gwtModule.getQualifiedName();
      boolean enabled = enabledModules.contains(moduleName);
      configuration.setModuleCompilationEnabled(moduleName, enabled);
    }
  }

  private void addGwtCompilerOutputToArtifact(IdeModifiableModelsProvider modelsProvider, Module module, GwtFacet facet,
                                              MavenProject project, WebFacet webFacet) {
    String gwtOutputPath = findValueInGoalConfigAndThenInPluginConfig(project, "compile", "webappDirectory");
    if (gwtOutputPath != null) {
      gwtOutputPath = FileUtil.toSystemIndependentName(gwtOutputPath);
    }

    PackagingModifiableModel packagingModel = modelsProvider.getModifiableModel(PackagingModifiableModel.class);
    final ModifiableArtifactModel artifactModel = packagingModel.getModifiableArtifactModel();
    final String artifactName = getArtifactName("war", module, true);
    final Artifact webArtifact = artifactModel.findArtifact(artifactName);

    String relativePath =
      GwtCompileOutputRelativePathSuggester.suggestRelativeOutputPath(facet, packagingModel.getPackagingElementResolvingContext());
    ModifiableArtifact targetArtifact = null;
    if (webArtifact != null) {
      final String artifactOutput = webArtifact.getOutputPath();
      targetArtifact = artifactModel.getOrCreateModifiableArtifact(webArtifact);

      if (!StringUtil.isEmpty(gwtOutputPath) && !StringUtil.isEmpty(artifactOutput)) {
        String gwtOutputRelativePath = getGwtOutputRelativePath(gwtOutputPath, artifactOutput, webFacet);
        if (gwtOutputRelativePath != null) {
          relativePath = gwtOutputRelativePath;
        }
        else {
          targetArtifact = null;
        }
      }
    }

    if (targetArtifact == null) {
      if (StringUtil.isEmpty(gwtOutputPath)) return;
      final String name = GwtFacetArtifactValidator.suggestArtifactName(module.getName());
      final Artifact existingGwtArtifact = artifactModel.findArtifact(name);
      if (existingGwtArtifact != null) {
        targetArtifact = artifactModel.getOrCreateModifiableArtifact(existingGwtArtifact);
      }
      else {
        targetArtifact = artifactModel.addArtifact(name, PlainArtifactType.getInstance());
      }
      targetArtifact.setOutputPath(gwtOutputPath);
    }

    final GwtCompilerOutputElement element = new GwtCompilerOutputElement(module.getProject(), facet);
    PackagingElementFactory.getInstance().getOrCreateDirectory(targetArtifact.getRootElement(), relativePath).addOrFindChild(element);
  }

  private static @Nullable String getGwtOutputRelativePath(String gwtOutputPath, String artifactOutput, WebFacet webFacet) {
    if (FileUtil.startsWith(gwtOutputPath, artifactOutput)) {
      return FileUtil.getRelativePath(artifactOutput, gwtOutputPath, '/');
    }
    for (WebRoot root : webFacet.getWebRoots()) {
      String webRootPath = VfsUtilCore.urlToPath(root.getDirectoryUrl());
      if (FileUtil.startsWith(gwtOutputPath, webRootPath)) {
        return FileUtil.getRelativePath(webRootPath, gwtOutputPath, '/');
      }
    }
    return null;
  }

  private @Nullable String findValueInGoalConfigAndThenInPluginConfig(MavenProject project, @NonNls String goal, @NonNls String path) {
    final String value = findGoalConfigValue(project, goal, path);
    if (value != null) {
      return value;
    }
    return findConfigValue(project, path);
  }

  protected void setupGwtSdk(GwtFacet facet, MavenProject project) {
    String gwtPath = findGwtPath(project);
    if (gwtPath != null) {
      gwtPath = FileUtil.toSystemIndependentName(gwtPath);
      gwtPath = VfsUtilCore.pathToUrl(gwtPath);
      facet.getConfiguration().setGwtSdkUrl(gwtPath);
    }
  }

  protected @Nullable String findGwtPath(MavenProject mavenProject) {
    String path = findConfigValue(mavenProject, "gwtHome");
    if (path == null) path = mavenProject.getProperties().getProperty("google.webtoolkit.home", null);
    return path;
  }

  protected @Nullable GwtPath resolveGwtPath(MavenProject project) {
    String sdkPath = findGwtPath(project);
    if (sdkPath != null) return new GwtPath(sdkPath, null, true);

    String version = null;
    for (MavenArtifact each : ContainerUtil.concat(project.findDependencies(GwtSdkPaths.OLD_GROUP_ID, GwtSdkPaths.GWT_USER_ARTIFACT_ID),
                                                   project.findDependencies(GwtSdkPaths.NEW_GROUP_ID, GwtSdkPaths.GWT_USER_ARTIFACT_ID),
                                                   project.findDependencies(GwtSdkPaths.OLD_GROUP_ID, "gwt-servlet"))) {
      version = each.getVersion();
      if (version != null) {
        break;
      }
    }
    if (version == null) return null;

    return new GwtPath(getGwtDevPath(project, version), version, false);
  }

  protected static @NonNls String getGwtDevPath(MavenProject project, String version) {
    boolean isGoogle = VersionComparatorUtil.compare(version, "2.10") < 0;

    if (isGoogle) {
      return project.getLocalRepositoryPath() + "/com/google/gwt/gwt-dev/" + version + "/";
    } else {
      return project.getLocalRepositoryPath() + "/org/gwtproject/gwt-dev/" + version + "/";
    }
  }

  public static @NonNls String getArtifactName(String packaging, Module module, boolean exploded) {
    final String baseName = module.getName() + ":" + packaging;
    return exploded ? baseName + " exploded" : baseName;
  }

  protected static class GwtPath {
    public final String path;
    public final String version;
    public final boolean isInstalled;

    public GwtPath(String path, String version, boolean installed) {
      this.path = path;
      this.version = version;
      this.isInstalled = installed;
    }
  }
}
