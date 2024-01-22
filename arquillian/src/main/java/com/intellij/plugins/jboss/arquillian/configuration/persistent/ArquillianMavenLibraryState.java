package com.intellij.plugins.jboss.arquillian.configuration.persistent;

import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianMavenCoordinates;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;

@Tag("maven-library")
public class ArquillianMavenLibraryState extends ArquillianLibraryState {
  @NotNull
  @Attribute("groupId")
  public String groupId;

  @NotNull
  @Attribute("artifactId")
  public String artifactId;

  @NotNull
  @Attribute("version")
  public String version;

  @Attribute("download-sources")
  public boolean downloadSources;

  @Attribute("download-java-docs")
  public boolean downloadJavaDocs;

  @SuppressWarnings("unused")
  private ArquillianMavenLibraryState() {
    this.groupId = "";
    this.artifactId = "";
    this.version = "";
  }

  public ArquillianMavenLibraryState(@NotNull String groupId,
                                     @NotNull String artifactId,
                                     @NotNull String version,
                                     boolean downloadSources,
                                     boolean downloadJavaDocs) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.downloadSources = downloadSources;
    this.downloadJavaDocs = downloadJavaDocs;
  }

  public ArquillianMavenLibraryState(@NotNull String groupId, @NotNull String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = RepositoryLibraryDescription.DefaultVersionId;
  }

  public ArquillianMavenLibraryState(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public ArquillianMavenLibraryState(ArquillianMavenCoordinates coordinates) {
    this(coordinates.getGroupId(), coordinates.getArtifactId());
  }

  @Override
  public <R> R accept(Visitor<R> visitor) {
    return visitor.visitMavenLibrary(this);
  }
}
