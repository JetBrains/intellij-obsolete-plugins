package com.intellij.bigdatatools.plugin.spark.gradle.tooling

import com.intellij.gradle.toolingExtension.impl.util.GradleTaskUtil
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.Message
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext

class ArchiveTaskModelBuilder : AbstractModelBuilderService() {

  override fun canBuild(modelName: String?): Boolean {
    return ArchiveTaskModel::class.java.name == modelName
  }

  override fun buildAll(modelName: String, project: Project, context: ModelBuilderContext): Any? {
    val archiveTaskToArtifact = collectArchiveTaskToArtifactMap(project)
    return DefaultArchiveTaskModel(archiveTaskToArtifact)
  }

  override fun reportErrorMessage(modelName: String, project: Project, context: ModelBuilderContext, exception: Exception) {
    context.messageReporter.createMessage()
      .withGroup(this)
      .withKind(Message.Kind.WARNING)
      .withTitle("ArtifactTaskModelBuilderImpl errors")
      .withException(exception)
      .reportMessage(project)
  }

  private fun collectArchiveTaskToArtifactMap(project: Project): Map<String, String> {
    val result = HashMap<String, String>()
    for (task in project.tasks.withType(AbstractArchiveTask::class.java)) {
      val archiveFile = GradleTaskUtil.getTaskArchiveFile(task)
      result[task.name] = archiveFile.path
    }
    return result
  }
}
