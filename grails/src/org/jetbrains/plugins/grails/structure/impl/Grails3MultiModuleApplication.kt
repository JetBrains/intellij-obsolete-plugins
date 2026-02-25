/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope.EMPTY_SCOPE
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData

internal class Grails3MultiModuleApplication(
    project: Project,
    root: VirtualFile,
    moduleDataNode: DataNode<ModuleData>
) : Grails3ApplicationBase(project, root, moduleDataNode) {

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    fun getScope(sourceSetName: String) = findModule(sourceSetName)?.let {
      if (includeDependencies) it.getModuleRuntimeScope(testsOnly) else it.getModuleScope(testsOnly)
    } ?: EMPTY_SCOPE

    return when {
      testsOnly -> getScope("test").uniteWith(getScope("integrationTest"))
      else -> getScope("main")
    }
  }

  private val mySourceSets by lazy {
    ExternalSystemApiUtil.findAll(moduleDataNode, GradleSourceSetData.KEY).map { it.data }
  }

  private fun findModule(sourceSetName: String): Module? {
    val sourceSet = mySourceSets.find { it.id.endsWith(":$sourceSetName") } ?: return null
    return ModuleManager.getInstance(project).modules.find { ExternalSystemApiUtil.getExternalProjectId(it) == sourceSet.id }
  }
}