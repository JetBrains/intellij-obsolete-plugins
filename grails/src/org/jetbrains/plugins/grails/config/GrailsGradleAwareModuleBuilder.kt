/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.config

import com.intellij.execution.ExecutionException
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder.setupGradleSettingsFile
import org.jetbrains.plugins.gradle.settings.DistributionType.DEFAULT_WRAPPED
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.runner.GrailsConsole
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.invariantSeparatorsPathString

abstract class GrailsGradleAwareModuleBuilder : ModuleBuilder() {
  companion object {
    val LOG = logger<GrailsGradleAwareModuleBuilder>()
  }

  var parentProject: ProjectData? = null
  var isCreatingNewProject: Boolean = false

  @Throws(ConfigurationException::class)
  override fun setupModule(module: Module) {
    super.setupModule(module)
    if (isCreatingNewProject) {
      module.project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, java.lang.Boolean.TRUE)
    }
  }

  protected fun linkModule(module: Module) {
    val project = module.project
    val root = getModuleRoot(module)

    val rootProjectPath: Path = parentProject?.linkedExternalProjectPath?.let { Paths.get(it) }
                          ?: (if (isCreatingNewProject) Paths.get(module.project.basePath!!) else root.toNioPath())

    val fresh = isCreatingNewProject || parentProject == null
    ApplicationManager.getApplication().runWriteAction {
      try {
        setupGradleSettingsFile(rootProjectPath, root, project.name, module.name, fresh, false)
        LOG.debug("'settings.gradle' file set up for module: $module")
      }
      catch (e: ConfigurationException) {
        LOG.debug(e)
        GrailsConsole.NOTIFICATION_GROUP
          .createNotification(GrailsBundle.message("failed.to.create.settings.gradle.notification.title"), e.message ?: "", NotificationType.WARNING)
          .notify(project)
      }
    }

    linkGradleProject(fresh, rootProjectPath, project)
  }

  private fun linkGradleProject(fresh: Boolean, rootProjectPath: Path, project: Project) {
    if (fresh) {
      val gradleSettings = GradleProjectSettings()
      gradleSettings.distributionType = DEFAULT_WRAPPED
      gradleSettings.externalProjectPath = rootProjectPath.invariantSeparatorsPathString
      gradleSettings.gradleJvm = "#USE_PROJECT_JDK"

      ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).linkProject(gradleSettings)
      LOG.debug("Project '$project' linked with $gradleSettings")
    }

    if (!isCreatingNewProject) {
      FileDocumentManager.getInstance().saveAllDocuments()
      LOG.debug("Starting refreshing the project: $rootProjectPath")
    }

    ExternalSystemUtil.refreshProject(rootProjectPath.invariantSeparatorsPathString, ImportSpecBuilder(project, GradleConstants.SYSTEM_ID))
  }

  @Throws(ExecutionException::class)
  protected fun getModuleRoot(module: Module): VirtualFile {
    val roots = ModuleRootManager.getInstance(module).contentRoots
    if (roots.size == 1) {
      return roots[0]
    }
    throw ExecutionException(GrailsBundle.message("dialog.message.no.module.root"))
  }
}
