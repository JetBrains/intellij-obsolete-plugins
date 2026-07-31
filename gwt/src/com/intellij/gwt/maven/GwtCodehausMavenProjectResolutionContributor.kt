package com.intellij.gwt.maven

import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectResolutionContributor
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper

class GwtCodehausMavenProjectResolutionContributor : MavenProjectResolutionContributor, GwtCodehausFacetImporter() {
  override suspend fun onMavenProjectResolved(
    project: Project,
    mavenProject: MavenProject,
    embedder: MavenEmbedderWrapper
  ) {
    if (!isApplicable(mavenProject)) return
    resolve(project, mavenProject, embedder)
  }
}