package com.intellij.plugins.jboss.arquillian.configuration.container;

public class ArquillianMavenCoordinates {
  private final String groupId;
  private final String artifactId;

  public ArquillianMavenCoordinates(String groupId, String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }
}
