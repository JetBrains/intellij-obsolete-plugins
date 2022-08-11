package com.intellij.vaadin.maven;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.maven.GwtCodehausFacetImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.ResolveContext;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinMavenSdkPaths;

import java.util.List;

public class GwtFacetForVaadinImporter extends GwtCodehausFacetImporter {
  public GwtFacetForVaadinImporter() {
    super("com.vaadin", "vaadin-maven-plugin");
  }

  @Override
  public void resolve(Project project, MavenProject mavenProject, NativeMavenProjectHolder nativeMavenProject,
                      MavenEmbedderWrapper embedder, ResolveContext context) throws MavenProcessCanceledException {
    String version = getVaadinVersion(mavenProject);
    if (version != null) {
      List<MavenRemoteRepository> repos = mavenProject.getRemoteRepositories();
      embedder.resolve(new MavenArtifactInfo("com.vaadin", "vaadin-client-compiler", version, "jar", null), repos);
    }
  }

  @Nullable
  private String getVaadinVersion(MavenProject mavenProject) {
    for (MavenArtifact artifact : mavenProject.findDependencies("com.vaadin", "vaadin-client")) {
      String artifactVersion = artifact.getVersion();
      if (artifactVersion != null) return artifactVersion;
    }
    return findConfigValue(mavenProject, "version");
  }

  @Override
  protected void setupGwtSdk(GwtFacet facet, MavenProject project) {
    String version = getVaadinVersion(project);
    if (version != null) {
      GwtFacetConfiguration configuration = facet.getConfiguration();
      String homePath = project.getLocalRepository().getPath() + "/com/vaadin/vaadin-client/" + version;
      configuration.setGwtSdkUrl(VfsUtilCore.pathToUrl(homePath));
      configuration.setGwtSdkType(GwtVaadinMavenSdkPaths.TYPE_ID);
    }
  }
}
