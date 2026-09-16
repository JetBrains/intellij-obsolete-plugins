package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.packaging.artifacts.ArtifactPointer
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorReturningOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class FileSelectorOptionArtifact : FileSelectorReturningOption(FileType.ARTIFACT,
                                                               SparkMessagesBundle.message("settings.url.artifact.name"),
                                                               SparkMessagesBundle.message("settings.url.artifact.tooltip"),
                                                               AllIcons.Nodes.Artifact) {
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    return selectSingleArtifact(context.project, context.prevSelectedPath)?.let {
      IdeaArtifactSelectedArtifactInfo(it, context.project)
    }
  }
  override val applicableForTypes get() = listOf(FileSelectorType.SSH_JAR, FileSelectorType.LOCAL_JAR, FileSelectorType.CLUSTER_JAR)

  private fun selectSingleArtifact(project: Project, prevValue: String?): ArtifactPointer? {
    val pointArtifacts = SelectArtifactDialog.sortedArtifactPointers(project)
    val prevSelectedArtifact = if (prevValue.isNullOrBlank()) null else pointArtifacts.firstOrNull { it.artifactName == prevValue }

    val dialog = SelectArtifactDialog(project, prevSelectedArtifact, pointArtifacts)
    val isSelected = dialog.show()

    if (!isSelected) return null
    return dialog.selectedArtifact
  }

}