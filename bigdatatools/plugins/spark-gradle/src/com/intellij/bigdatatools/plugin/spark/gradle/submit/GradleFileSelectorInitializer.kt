package com.intellij.bigdatatools.plugin.spark.gradle.submit

import com.intellij.execution.BeforeRunTask
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorReturningOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.BuildAndUploadSelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.FileSelectorInitializer
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import icons.GradleIcons
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleTaskData

class GradleFileSelectorInitializer : FileSelectorInitializer {

  override fun guessFromContext(project: Project, file: PsiFile): SelectedArtifactInfo? {
    val artifacts = computeArtifacts(project)
    val defaultArtifact = artifacts.entries.minByOrNull { !it.key.contains("shadow") } ?: return null
    val (artifact, tasks) = defaultArtifact
    val task = tasks.firstOrNull()
    return GradleSelectedArtifactInfo(artifact, task?.name, task?.data?.linkedExternalProjectPath)
  }

}

class GradleSelectedArtifactInfo(
  artifactPath: String,
  private val taskName: String?,
  private val externalProjectPath: String?
) : BuildAndUploadSelectedArtifactInfo(FilePath(FileType.UPLOAD, artifactPath)) {
  override fun isMyBuildTask(task: BeforeRunTask<*>) =
    taskName != null && externalProjectPath != null &&
    task.providerId == GradleBeforeRunTaskProvider.ID &&
    task is ExternalSystemBeforeRunTask &&
    task.taskExecutionSettings.taskNames.contains(taskName) &&
    task.taskExecutionSettings.externalProjectPath == externalProjectPath

  override fun createBuildTask(): BeforeRunTask<*>? {
    if (externalProjectPath == null || taskName == null) return null
    val task = ExternalSystemBeforeRunTask(GradleBeforeRunTaskProvider.ID, GradleConstants.SYSTEM_ID)
    task.taskExecutionSettings.externalProjectPath = externalProjectPath
    task.taskExecutionSettings.taskNames = listOf(taskName)
    return task
  }
}

class FileSelectorOptionGradle : FileSelectorReturningOption(
  fileType = FileType.UPLOAD,
  title = SparkMessagesBundle.message("settings.url.gradle.artifact.name"),
  tooltip = SparkMessagesBundle.message("settings.url.gradle.artifact.tooltip"),
  icon = GradleIcons.GradleFile
) {
  override fun isAvailable(project: Project): Boolean {
    val artifacts = computeArtifacts(project)
    return artifacts.isNotEmpty()
  }
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    val artifacts = computeArtifacts(context.project)
    val defaultArtifact = artifacts.entries.minByOrNull { !it.key.contains("shadow") }
    val initialSelected = context.prevSelectedArtifactInfo<GradleSelectedArtifactInfo>()?.filePath?.path
    return showGradleTaskDialog(context.project, artifacts.keys.toList(), initialSelected, defaultArtifact?.key)
  }
  override val applicableForTypes get() = listOf(FileSelectorType.CLUSTER_JAR)
}

private fun computeArtifacts(project: Project): Map<String, List<GradleTaskData>> {
  val externalProjects = ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID).mapNotNull {
    ExternalProjectDataCache.getInstance(project).getRootExternalProject(it.externalProjectPath)
  }
  return externalProjects.flatMap { externalProject ->
    listOf(externalProject) + externalProject.childProjects.values
  }.flatMap { externalProject ->
    val artifactsToTasks = ExternalSystemApiUtil.findModuleNode(project, GradleConstants.SYSTEM_ID,
                                                                externalProject.projectDir.path)?.let { moduleDataNode ->
      ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.TASK).flatMap { taskNode ->
        ExternalSystemApiUtil.findAll(taskNode, ArtifactTaskData.KEY).mapNotNull { artifactNode ->
          artifactNode.data.artifact
        }.map { artifact ->
          artifact to GradleTaskData(taskNode, externalProject.path)
        }
      }
    }
    artifactsToTasks?.takeIf { it.isNotEmpty() } ?: externalProject.artifacts.map { it.path to null }
  }.groupingBy { it.first }.fold(emptyList()) { acc, (_, new) -> acc + listOfNotNull(new) }
}

private fun showGradleTaskDialog(
  project: Project,
  artifacts: List<String>,
  initialSelected: String?,
  defaultInitial: String?
): GradleSelectedArtifactInfo? {
  return SelectGradleTaskDialog(
    project = project,
    myTitle = SparkMessagesBundle.message("dialog.title.select.gradle.artifact.with.task"),
    artifacts = artifacts,
    initialSelected = initialSelected.takeIf { artifacts.contains(initialSelected) } ?: defaultInitial
  ).showAndGetResult()
}
