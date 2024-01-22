package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.jarRepository.RepositoryLibraryType;
import com.intellij.jarRepository.settings.RepositoryLibraryPropertiesDialog;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianMavenLibraryState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;
import org.jetbrains.idea.maven.utils.library.propertiesEditor.RepositoryLibraryPropertiesModel;

import javax.swing.*;

public class ArquillianMavenLibraryModel extends ArquillianLibraryModel {
  @NotNull private final String groupId;

  @NotNull private final String artifactId;

  @NotNull private String version;

  private boolean downloadSources;

  private boolean downloadJavaDocs;

  public ArquillianMavenLibraryModel(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public ArquillianMavenLibraryModel(ArquillianMavenLibraryState state) {
    groupId = state.groupId;
    artifactId = state.artifactId;
    version = state.version;
    downloadSources = state.downloadSources;
    downloadJavaDocs = state.downloadJavaDocs;
  }

  @Override
  public boolean hasChanges(ArquillianLibraryState state) {
    if (!(state instanceof ArquillianMavenLibraryState mavenLibraryState)) {
      return true;
    }
    return !groupId.equals(mavenLibraryState.groupId)
           || !artifactId.equals(mavenLibraryState.artifactId)
           || !version.equals(mavenLibraryState.version)
           || downloadSources != mavenLibraryState.downloadSources
           || downloadJavaDocs != mavenLibraryState.downloadJavaDocs;
  }

  @Override
  public ArquillianMavenLibraryState getCurrentState() {
    return new ArquillianMavenLibraryState(groupId, artifactId, version, downloadSources, downloadJavaDocs);
  }

  @NotNull
  public String getGroupId() {
    return groupId;
  }

  @NotNull
  public String getArtifactId() {
    return artifactId;
  }

  @NotNull
  public String getVersion() {
    return version;
  }

  public void setVersion(@NotNull String version) {
    this.version = version;
    notifyMeChanged();
  }

  public boolean isDownloadSources() {
    return downloadSources;
  }

  public void setDownloadSources(boolean downloadSources) {
    this.downloadSources = downloadSources;
    notifyMeChanged();
  }

  public boolean isDownloadJavaDocs() {
    return downloadJavaDocs;
  }

  public void setDownloadJavaDocs(boolean downloadJavaDocs) {
    this.downloadJavaDocs = downloadJavaDocs;
    notifyMeChanged();
  }

  @Override
  public String getDescription() {
    return RepositoryLibraryDescription.findDescription(groupId, artifactId).getDisplayName(version);
  }

  @Override
  public Icon getIcon() {
    return RepositoryLibraryType.getInstance().getIcon(new RepositoryLibraryProperties(groupId, artifactId, version));
  }

  @Override
  public void editProperties(Project project) {
    RepositoryLibraryDescription libraryDescription =
      RepositoryLibraryDescription.findDescription(groupId, artifactId);
    RepositoryLibraryPropertiesModel model = new RepositoryLibraryPropertiesModel(
      version,
      downloadSources,
      downloadJavaDocs);
    RepositoryLibraryPropertiesDialog dialog = new RepositoryLibraryPropertiesDialog(
      project,
      model,
      libraryDescription,
      true, false);
    if (!dialog.showAndGet()) {
      return;
    }

    version = model.getVersion();
    downloadSources = model.isDownloadSources();
    downloadJavaDocs = model.isDownloadJavaDocs();
    notifyMeChanged();

    JarRepositoryManager.loadDependenciesAsync(
      project,
      new RepositoryLibraryProperties(groupId, artifactId, version),
      downloadSources,
      downloadJavaDocs,
      null, null
    );
  }
}
