package org.jetbrains.jps.gwt.model;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class MavenCoordinates implements Serializable {
  public final @NotNull String groupId;
  public final @NotNull String artifactId;
  public final @NotNull String version;

  public MavenCoordinates(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }
}
