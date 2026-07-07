package org.jetbrains.plugins.ruby.chef.sourceRoot

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.stream
import org.jetbrains.plugins.ruby.ruby.RModuleUtil
import java.util.stream.Collectors

@Service(Service.Level.PROJECT)
class CookbookUrlsCache(project: Project) {
  private var cache: CachedValue<MutableMap<Module, List<String>>> = CachedValuesManager.getManager(project).createCachedValue(
    { CachedValueProvider.Result.create(fillCache(project), ProjectRootModificationTracker.getInstance(project)) }, false)

  fun getCachedURLs(module: Module): List<String> = let { cache.value[module] } ?: emptyList()

  private fun fillCache(project: Project): MutableMap<Module, List<String>> =
    RModuleUtil.getInstance().getAllModulesWithRubySupport(project).stream().collect(Collectors.toMap(
      { it }, {
      ModuleRootManager.getInstance(it).contentEntries
        .flatMap { contentEntry -> contentEntry.getSourceFolders(CookbooksRootType.COOKBOOKS) }
        .map { it.url }
    }))

  companion object {
    fun getInstance(project: Project): CookbookUrlsCache = project.getService(CookbookUrlsCache::class.java)
  }
}