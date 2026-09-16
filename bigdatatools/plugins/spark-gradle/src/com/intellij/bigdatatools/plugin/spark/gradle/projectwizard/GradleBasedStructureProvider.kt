package com.intellij.bigdatatools.plugin.spark.gradle.projectwizard

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProvider
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.project.open.setupGradleSettings
import org.jetbrains.plugins.gradle.settings.GradleDefaultProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.setupGradleJvm
import org.jetbrains.plugins.gradle.util.suggestGradleVersion
import org.jetbrains.plugins.gradle.util.validateJavaHome
import java.io.File
import java.nio.file.Path

internal class GradleBasedStructureProvider(
  projectDir: File, errorHandler: (Exception) -> Unit) : BuildSystemBasedStructureProvider(projectDir, errorHandler) {
  override val simpleName: String = "Gradle"
  override val buildFiles: Set<String> = setOf("build.gradle", "settings.gradle")

  override fun prefixSrc() = "src/main/java"

  override fun linkProject(project: Project, contentEntryPath: String, progressIndicator: ProgressIndicator) {
    val externalProjectPath = project.basePath!!
    val settings = GradleSettings.getInstance(project)
    val projectSettings = GradleDefaultProjectSettings.createProjectSettings(externalProjectPath)

    settings.setupGradleSettings()

    val gradleVersion = suggestGradleVersion {
      withProject(project)
      withProjectJdkVersionFilter(project)
    } ?: GradleVersion.current()

    setupGradleJvm(project, projectSettings, gradleVersion)
    validateJavaHome(project, Path.of(externalProjectPath), gradleVersion)
    settings.linkProject(projectSettings)

    project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, true)
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)

    ApplicationManager.getApplication().invokeLater {
      ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
        val importSpec = ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
        importSpec.createDirectoriesForEmptyContentRoots()
        ExternalSystemUtil.refreshProject(externalProjectPath, importSpec)
      }
    }
  }

  override fun originalSkeletonUrl(): String? = null

  override fun resourceSkeletonUrl(): String = "templates/gradle-template.zip"
}