package com.intellij.plugins.jboss.arquillian.libraries;

import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;

import java.util.HashMap;
import java.util.Map;

public final class ArquillianLibrarySynchronizer implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(() -> {
      final Map<String, ArquillianMavenLibraryState> libsToDownload = new HashMap<>();
      for (ArquillianContainerState container : ArquillianContainersManager.getInstance(project).getState().containers) {
        for (ArquillianLibraryState library : container.libraries) {
          library.accept(new ArquillianLibraryState.Visitor<Void>() {
            @Override
            public Void visitMavenLibrary(ArquillianMavenLibraryState mavenLibrary) {
              String mavenId = mavenLibrary.groupId + ":" + mavenLibrary.artifactId + ":" + mavenLibrary.version;
              ArquillianMavenLibraryState mapLibrary = libsToDownload.get(mavenId);
              if (mapLibrary == null) {
                mapLibrary = new ArquillianMavenLibraryState(mavenLibrary.groupId, mavenLibrary.artifactId, mavenLibrary.version);
                libsToDownload.put(mavenId, mapLibrary);
              }
              mapLibrary.downloadSources = mapLibrary.downloadSources || mavenLibrary.downloadSources;
              mapLibrary.downloadJavaDocs = mapLibrary.downloadJavaDocs || mavenLibrary.downloadJavaDocs;
              return null;
            }

            @Override
            public Void visitExistLibrary(ArquillianExistLibraryState state) {
              return null;
            }
          });
        }
      }
      for (ArquillianMavenLibraryState mavenLibraryState : libsToDownload.values()) {
        JarRepositoryManager.loadDependenciesAsync(
          project,
          new RepositoryLibraryProperties(mavenLibraryState.groupId, mavenLibraryState.artifactId, mavenLibraryState.version),
          mavenLibraryState.downloadSources,
          mavenLibraryState.downloadJavaDocs,
          null, null
        );
      }
    }, project.getDisposed());
  }
}
