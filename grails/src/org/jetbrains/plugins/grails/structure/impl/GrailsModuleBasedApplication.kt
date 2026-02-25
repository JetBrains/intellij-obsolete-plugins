/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.plugins.grails.structure.GrailsApplicationBase

abstract class GrailsModuleBasedApplication(
  val module: Module,
  root: VirtualFile
) : GrailsApplicationBase(module.project, root) {

  override val isValid: Boolean get() = super.isValid && !module.isDisposed

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    return when {
      testsOnly && includeDependencies -> module.moduleTestsWithDependentsScope
      testsOnly && !includeDependencies -> CachedValuesManager.getManager(project).getCachedValue(this) {
        val moduleScope = module.getModuleScope(true)
        val moduleWithoutTestsScope = module.getModuleScope(false)
        val result = moduleScope.intersectWith(GlobalSearchScope.notScope(moduleWithoutTestsScope))
        Result.create(result, ProjectRootManager.getInstance(project))
      }
      !testsOnly && includeDependencies -> module.getModuleRuntimeScope(false)
      !testsOnly && !includeDependencies -> module.getModuleScope(false)
      else -> throw RuntimeException("never happens")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    if (!super.equals(other)) return false

    other as GrailsModuleBasedApplication

    return module == other.module
  }

  override fun hashCode(): Int = 31 * super.hashCode() + module.hashCode()
}