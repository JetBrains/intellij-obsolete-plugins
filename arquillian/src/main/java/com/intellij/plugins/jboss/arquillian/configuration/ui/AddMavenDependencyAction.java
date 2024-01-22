package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianLibraryModel;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianMavenLibraryModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

public class AddMavenDependencyAction extends AddLibraryAction {
  private final @NotNull Project project;
  private final @NotNull JComponent parentComponent;

  public AddMavenDependencyAction(@NotNull Project project, @NotNull JComponent parentComponent) {
    super(RepositoryLibraryDescription.DEFAULT_ICON, ArquillianBundle.message("arquillian.action.add.maven.dependency"));
    this.project = project;
    this.parentComponent = parentComponent;
  }

  @Override
  Collection<ArquillianLibraryModel> execute() {
    NewLibraryConfiguration configuration = JarRepositoryManager.chooseLibraryAndDownload(project, null, parentComponent);
    if (configuration != null) {
      RepositoryLibraryProperties libraryProperties = (RepositoryLibraryProperties)configuration.getProperties();
      return Collections.singletonList(new ArquillianMavenLibraryModel(
        libraryProperties.getGroupId(),
        libraryProperties.getArtifactId(),
        libraryProperties.getVersion()));
    }
    else {
      return Collections.emptyList();
    }
  }
}
