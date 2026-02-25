/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.findModule
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.jetbrains.plugins.grails.gradle.GrailsModuleData
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider

class Grails3ApplicationProvider : GrailsApplicationProvider() {

  override fun createApplication(project: Project, root: VirtualFile): GrailsApplication? {
    val path = root.path

    val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
    val linkedProjectSettings = settings.getLinkedProjectSettings(path) as? GradleProjectSettings ?: return null
    val gradleProjectInfo = ProjectDataManager.getInstance().getExternalProjectData(
        project, SYSTEM_ID, linkedProjectSettings.externalProjectPath
    ) ?: return null

    val moduleData = findModule(gradleProjectInfo.externalProjectStructure, path) ?: return null
    if (ExternalSystemApiUtil.find(moduleData, GrailsModuleData.KEY) == null) return null

    return if (linkedProjectSettings.isResolveModulePerSourceSet) {
      Grails3MultiModuleApplication(project, root, moduleData)
    }
    else {
      ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(root)?.let { module ->
        Grails3SingleModuleApplication(module, root, moduleData)
      }
    }
  }
}
