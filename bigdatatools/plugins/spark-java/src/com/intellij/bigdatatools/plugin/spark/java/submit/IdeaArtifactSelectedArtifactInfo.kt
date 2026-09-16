package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.execution.BeforeRunTask
import com.intellij.openapi.project.Project
import com.intellij.packaging.artifacts.ArtifactPointer
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.cluster.BuildAndUploadSelectedArtifactInfo

class IdeaArtifactSelectedArtifactInfo(private val artifactPointer: ArtifactPointer, val project: Project) : BuildAndUploadSelectedArtifactInfo(
  FilePath(FileType.ARTIFACT, artifactPointer.artifactName)) {
  override fun isMyBuildTask(task: BeforeRunTask<*>): Boolean {
    val myArtifact = artifactPointer.artifact
    return (task.providerId == BuildArtifactsBeforeRunTaskProvider.ID &&
            task is BuildArtifactsBeforeRunTask
            && task.artifactPointers.any { myArtifact != null && it.artifact?.name == myArtifact.name })
  }

  override fun createBuildTask(): BeforeRunTask<*> {
    return BuildArtifactsBeforeRunTask(project).apply {
      artifactPointers = listOf(artifactPointer)
    }
  }
}