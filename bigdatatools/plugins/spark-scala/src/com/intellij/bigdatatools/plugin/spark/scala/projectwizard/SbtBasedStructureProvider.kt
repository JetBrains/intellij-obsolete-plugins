package com.intellij.bigdatatools.plugin.spark.scala.projectwizard

import com.intellij.build.BuildContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProvider
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.SbtProjectSettings
import java.io.File

class SbtBasedStructureProvider(
  projectDir: File, errorHandler: (Exception) -> Unit
) : BuildSystemBasedStructureProvider(projectDir, errorHandler) {
  override val simpleName: String = "SBT"
  override val buildFiles: Set<String> = setOf("build.sbt", "build.properties")

  override fun prefixSrc() = "src/main/scala"

  override fun linkProject(project: Project, contentEntryPath: String, progressIndicator: ProgressIndicator) {
    (ExternalSystemApiUtil.getSettings(project, SbtProjectSystem.Id)
      as? AbstractExternalSystemSettings<*, SbtProjectSettings, *>)?.linkProject(createSbtProjectSettings(contentEntryPath))

    ApplicationManager.getApplication().invokeLater {
      ExternalProjectsManagerImpl.getInstance(project).init()
      ExternalSystemUtil.refreshProjects(
        ImportSpecBuilder(project, SbtProjectSystem.Id)
          .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      )
    }

    StartupManager.getInstance(project).runAfterOpened {
      ApplicationManager.getApplication().invokeLater {
        BuildContentManager.getInstance(project).orCreateToolWindow.show()
      }
    }
  }

  override fun originalSkeletonUrl(): String? = null

  override fun resourceSkeletonUrl(): String = "templates/sbt-template.zip"

  private fun createSbtProjectSettings(contentEntryPath: String) = SbtProjectSettings.default().apply {
    this.externalProjectPath = contentEntryPath
  }

  private fun updateModuleFilePath(pathname: String): String {
    val file = File(pathname)
    return file.parent + "/" + Sbt.ModulesDirectory() + "/" + file.name.lowercase()
  }
}