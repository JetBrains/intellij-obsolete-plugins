/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gorm

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.plugins.grails.gorm.GormClassNames.ENTITY_ANNO
import org.jetbrains.plugins.grails.gorm.GormClassNames.ENTITY_TRAIT
import org.jetbrains.plugins.grails.gorm.GormClassNames.QUERY_CREATOR
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor.GORM_INSTANCE_API_CLASS

enum class GormVersion {
  /**
   * GormApi compile time injection
   */
  BELOW_4,
  /**
   * GormEntity trait injection
   */
  IS_4,
  /**
   * GormEntity trait injection with custom traits for different DBs (Mongo, Cassandra, etc)
   */
  IS_5,
  /**
   * Custom traits for different DBs which are checked for availability in current context
   */
  IS_6
}

fun getGormVersion(element: PsiElement?): GormVersion? = element?.let {
  getGormVersion(ModuleUtilCore.findModuleForPsiElement(it))
}

fun getGormVersion(module: Module?): GormVersion? = module?.let {
  CachedValuesManager.getManager(it.project).getCachedValue(it) {
    Result.create(doGetGormVersion(it), ProjectRootManager.getInstance(it.project))
  }
}

private fun doGetGormVersion(module: Module): GormVersion? {
  val project = module.project
  val facade = JavaPsiFacade.getInstance(project)
  val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)

  // present since GORM 6
  facade.findClass(QUERY_CREATOR, scope)?.let {
    return GormVersion.IS_6
  }

  // present since GORM 5
  facade.findClass(ENTITY_ANNO, scope)?.let {
    return GormVersion.IS_5
  }

  // present since GORM 4
  facade.findClass(ENTITY_TRAIT, scope)?.let {
    return GormVersion.IS_4
  }

  // GormInstanceApi class is present in all versions, so we check it last
  facade.findClass(GORM_INSTANCE_API_CLASS, scope)?.let {
    return GormVersion.BELOW_4
  }

  return null
}