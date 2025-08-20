package com.intellij.plugins.jboss.arquillian.configuration.container;

import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianExistLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianMavenLibraryState;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;

public final class ArquillianContainerUtils {
  public static void loadDependenciesAsync(final Project project,
                                           final ArquillianContainerState state) {
    for (final ArquillianLibraryState libraryState : state.libraries) {
      libraryState.accept(new ArquillianLibraryState.Visitor<Void>() {
        @Override
        public Void visitMavenLibrary(ArquillianMavenLibraryState state) {
          JarRepositoryManager.loadDependenciesAsync(
            project,
            new RepositoryLibraryProperties(state.groupId, state.artifactId, state.version),
            state.downloadSources,
            state.downloadJavaDocs,
            null, null
          );
          return null;
        }

        @Override
        public Void visitExistLibrary(ArquillianExistLibraryState state) {
          return null;
        }
      });
    }
  }
}
