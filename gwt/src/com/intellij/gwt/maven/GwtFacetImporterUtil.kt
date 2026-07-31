package com.intellij.gwt.maven

import org.jetbrains.idea.maven.model.MavenArtifact
import org.jetbrains.idea.maven.model.MavenArtifactInfo
import org.jetbrains.idea.maven.model.MavenRemoteRepository
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper
import org.jetbrains.jps.gwt.model.GwtSdkPaths

class GwtFacetImporterUtil {
  companion object {
    @JvmStatic
    fun resolveArtifactTransitively(artifactId: String,
                                    gwtVersion: String,
                                    classifier: String?,
                                    embedder: MavenEmbedderWrapper,
                                    repos: List<MavenRemoteRepository>): Collection<MavenArtifact> {
      val oldGroupArtifact = MavenArtifactInfo(GwtSdkPaths.OLD_GROUP_ID, artifactId, gwtVersion, "jar", classifier)
      val newGroupArtifact = MavenArtifactInfo(GwtSdkPaths.NEW_GROUP_ID, artifactId, gwtVersion, "jar", classifier)
      return embedder.resolveArtifactTransitively(listOf(oldGroupArtifact, newGroupArtifact), repos).mavenResolvedArtifacts
    }
  }
}