// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.helidon.config.HelidonConfigValueSearcher.HelidonConfigValueResult
import com.intellij.helidon.config.HelidonConfigValueSearcher.HelidonConfigValueSearchParams
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.util.CommonProcessors
import com.intellij.util.Processor
import com.intellij.util.SmartList
import org.jetbrains.jps.model.java.JavaResourceRootType

abstract class HelidonConfigFileContributor(val fileType: FileType) {

  companion object {
    val EP_NAME: ExtensionPointName<HelidonConfigFileContributor> = ExtensionPointName.create("com.intellij.helidon.configFileContributor")

    internal fun findConfigFiles(module: Module, includeTests: Boolean): List<Pair<VirtualFile, HelidonConfigFileContributor>> {
      val result = SmartList<Pair<VirtualFile, HelidonConfigFileContributor>>()
      val extensionList = EP_NAME.extensionList
      for (contributor in extensionList) {
        val configFiles = contributor.findConfigFiles(module, includeTests)
        result.addAll(configFiles.map { Pair(it, contributor) })
      }
      return result
    }
  }

  abstract fun findKey(psiFile: PsiFile, key: String): PsiElement?

  abstract fun getKeyVariants(psiFile: PsiFile): Collection<LookupElement>

  abstract fun processConfigValues(params: HelidonConfigValueSearchParams, processor: Processor<in HelidonConfigValueResult>): Boolean

  fun findConfigFiles(module: Module, includeTests: Boolean): List<VirtualFile> {
    val configFileSearchScope = getConfigFileSearchScope(module, includeTests) ?: return emptyList()

    val result = SmartList<VirtualFile>()
    val processor = object : CommonProcessors.CollectProcessor<VirtualFile>(result) {
      override fun accept(file: VirtualFile): Boolean {
        ProgressManager.checkCanceled()
        return isHelidonConfigFileName(file.nameWithoutExtension)
      }
    }
    FileTypeIndex.processFiles(fileType, processor, configFileSearchScope)
    return result
  }

  private fun getConfigFileSearchScope(module: Module, testScope: Boolean): GlobalSearchScope? {
    if (module.isDisposed) {
      return null
    }
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return module.getModuleScope(testScope)
    }
    val configDirectories = LinkedHashSet<VirtualFile>()
    collectConfigDirectories(module, configDirectories, testScope)

    return if (configDirectories.isEmpty()) {
      null
    }
    else GlobalSearchScopesCore.directoriesScope(module.project, false, *configDirectories.toArray(VirtualFile.EMPTY_ARRAY))
  }

  private fun collectConfigDirectories(module: Module,
                                       configDirectories: MutableSet<VirtualFile>,
                                       testScope: Boolean,
                                       visitedModules: MutableSet<in Module> = HashSet()) {
    if (visitedModules.contains(module)) return

    visitedModules.add(module)

    val moduleRootManager = ModuleRootManager.getInstance(module)
    configDirectories.addAll(
      moduleRootManager.getSourceRoots(if (testScope) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE))

    for (dependentModule in moduleRootManager.getDependencies(testScope)) {
      collectConfigDirectories(dependentModule, configDirectories, testScope, visitedModules)
    }
  }
}