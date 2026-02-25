/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

internal class Grails3SingleModuleApplication(
    val module: Module,
    root: VirtualFile,
    moduleDataNode: DataNode<ModuleData>
) : Grails3ApplicationBase(module.project, root, moduleDataNode) {

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    return if (includeDependencies) {
      module.getModuleRuntimeScope(testsOnly)
    }
    else {
      module.getModuleScope(testsOnly)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    if (!super.equals(other)) return false

    other as Grails3SingleModuleApplication

    return module == other.module
  }

  override fun hashCode() = 31 * super.hashCode() + module.hashCode()
}
