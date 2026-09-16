package com.intellij.bigdatatools.plugin.spark.maven.submit

import com.intellij.execution.BeforeRunTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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
import icons.MavenIcons
import org.jetbrains.idea.maven.navigator.SelectMavenProjectDialog
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider
import javax.swing.Action

internal class MavenFileSelectorInitializer : FileSelectorInitializer {

  class MavenSelectedArtifactInfo(
    artifactPath: String,
    private val goal: String?,
    private val projectPath: String?
  ) : BuildAndUploadSelectedArtifactInfo(FilePath(FileType.UPLOAD, artifactPath)) {
    override fun isMyBuildTask(task: BeforeRunTask<*>) =
      goal != null && projectPath != null &&
      task.providerId == MavenBeforeRunTasksProvider.ID &&
      task is MavenBeforeRunTask &&
      task.goal == goal &&
      task.projectPath == projectPath

    override fun createBuildTask(): BeforeRunTask<*>? {
      if (projectPath == null || goal == null) return null
      val task = MavenBeforeRunTask()
      task.projectPath = projectPath
      task.goal = goal
      return task
    }
  }

  override fun guessFromContext(project: Project, file: PsiFile): SelectedArtifactInfo? {
    val virtualFile: VirtualFile = file.virtualFile ?: return null
    val mavenProject = MavenProjectsManager.getInstance(project).findContainingProject(virtualFile) ?: return null
    return getMavenArtifact(mavenProject)
  }

}

class FileSelectorOptionMaven : FileSelectorReturningOption(FileType.UPLOAD,
                                                            SparkMessagesBundle.message("settings.url.maven.artifact.name"),
                                                            SparkMessagesBundle.message("settings.url.maven.artifact.tooltip"),
                                                            MavenIcons.ExecuteMavenGoal) {
  override fun isAvailable(project: Project): Boolean {
    return MavenProjectsManager.getInstance(project).isMavenizedProject
  }
  override fun select(context: FileSelectorContext): SelectedArtifactInfo? {
    val dialog = object : SelectMavenProjectDialog(context.project, null) {
      override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
      }
    }
    dialog.showAndGet()
    val selectedProject = dialog.result ?: return null
    return getMavenArtifact(selectedProject)
  }
  override val applicableForTypes get() = listOf(FileSelectorType.CLUSTER_JAR)
}

private fun getMavenArtifact(mavenProject: MavenProject): MavenFileSelectorInitializer.MavenSelectedArtifactInfo? {
  if (mavenProject.packaging != "jar") return null
  val finalName = mavenProject.finalName
  val buildDirectory = mavenProject.buildDirectory
  val jarPlugin = mavenProject.findPlugin("org.apache.maven.plugins", "maven-jar-plugin") ?: return null
  val classifier = jarPlugin.configurationElement?.getChild("classifier")?.text?.trim()?.takeIf { it.isNotEmpty() }
  val artifactPath = "$buildDirectory/$finalName${classifier?.let { "-$classifier" }.orEmpty()}.jar"
  return MavenFileSelectorInitializer.MavenSelectedArtifactInfo(artifactPath, "package", mavenProject.path)
}
