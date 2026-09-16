package com.intellij.bigdatatools.zeppelin.dependency.resolver

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.ConnectionSettingsListener
import com.intellij.bigdatatools.zeppelin.cache.ZeppelinDependencyCacheManager
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.collector.user.ZeppelinUserDependenciesStorage
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl.ZeppelinCachedDependencyResolver
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl.ZeppelinLocalFileDependencyResolver
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl.ZeppelinMavenDependencyResolver
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl.ZeppelinModuleDependencyResolver
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import org.jetbrains.annotations.TestOnly

@Service
class ZepDepRootsResolverService : Disposable {
  private val storageManager = ZeppelinDependencyCacheManager()

  private val resolverImpls = setOf(
    ZeppelinCachedDependencyResolver(ZeppelinMavenDependencyResolver()),
    ZeppelinLocalFileDependencyResolver(),
    ZeppelinModuleDependencyResolver())

  init {
    resolverImpls.forEach {
      Disposer.register(this, it)
    }

    RfsConnectionDataManager.instance?.addListener(object : ConnectionSettingsListener {
      override fun onConnectionRemoved(project: Project?, removedConnectionData: ConnectionData) {
        if (removedConnectionData !is ZeppelinConnectionData) return

        val id = removedConnectionData.innerId

        val realProject = if (removedConnectionData.isPerProject)
          project
        else
          null
        storageManager.clearCachesForId(realProject, id)
        //TODO: remove it from it
        ZeppelinUserDependenciesStorage.clearDependencies(id)
      }
    })
  }

  fun resolveOrGetFromCache(libs: List<ZeppelinLibrary>,
                            repos: List<Repository>,
                            project: Project?,
                            configId: String,
                            indicator: ProgressIndicator,
                            force: Boolean): List<List<OrderRoot>> {
    val total = libs.size

    val allCachedRoots = libs.map {
      getRootsFromCache(library = it, project = project, configId = configId)
    }

    val realDownloadSize = allCachedRoots.count { it == null }
    if (realDownloadSize == 0)
      return allCachedRoots.filterNotNull()

    var indexOfResolvedDeps = 0

    return libs.zip(allCachedRoots).map { (lib, cachedRoots) ->
      if (cachedRoots != null)
        return@map cachedRoots

      if (indicator.isCanceled)
        return emptyList()

      indicator.text = "(${indexOfResolvedDeps + 1}/${realDownloadSize}) " +
                       "${ZepMessagesBundle.message("dependency.synchronization.resolve.dep")}: ${lib.presentableName}"
      indicator.isIndeterminate = false
      indicator.fraction = (indexOfResolvedDeps.toDouble()) / total
      indexOfResolvedDeps++

      resolveRoots(library = lib, repositories = repos, project = project, configId = configId, force = force, indicator = indicator)
    }
  }

  private fun getRootsFromCache(library: ZeppelinLibrary,
                                project: Project?,
                                configId: String): List<OrderRoot>? {
    val cachedRoots = storageManager.getSavedRoots(library, project, configId)

    return if (cachedRoots != null && cachedRoots.isNotEmpty())
      cachedRoots
    else
      null
  }

  fun resolveRoots(library: ZeppelinLibrary,
                   repositories: List<Repository>,
                   project: Project?,
                   configId: String,
                   force: Boolean = false,
                   indicator: ProgressIndicator?): List<OrderRoot> {
    val cachedRoots = storageManager.getSavedRoots(library, project, configId)
    if (cachedRoots != null && cachedRoots.isNotEmpty())
      return cachedRoots

    val roots = downloadRoots(library.dep, project, repositories, force, indicator)
    if (roots.isNotEmpty())
      storageManager.saveValues(library, roots, project, configId)

    return roots
  }

  override fun dispose() {}

  private fun downloadRoots(dep: ZepDependency,
                            project: Project?,
                            repositories: List<Repository>,
                            force: Boolean,
                            indicator: ProgressIndicator?): List<OrderRoot> {
    val resolver = gerResolver(dep)

    val classesRoots = try {
      resolver.resolveRoots(project, dep, repositories, OrderRootType.CLASSES, force, indicator)
    }
    catch (t: Throwable) {
      logger.warn("Resolve classes for ${dep.groupArtifactVersion} error", t)
      return emptyList()
    }

    if (indicator?.isCanceled == true)
      return emptyList()

    val sourceRoots = try {
      if (classesRoots.isNotEmpty())
        resolver.resolveRoots(project, dep, repositories, OrderRootType.SOURCES, force, indicator)
      else
        emptyList()
    }
    catch (t: Throwable) {
      logger.warn("Resolve sources for ${dep.groupArtifactVersion} error", t)
      return emptyList()
    }

    return classesRoots + sourceRoots
  }

  fun gerResolver(dep: ZepDependency) =
    resolverImpls.find { it.accept(dep) } ?: error("Dep resolver not found")

  @TestOnly
  fun clearCaches() {
    resolverImpls.forEach {
      Disposer.dispose(it)
    }
    storageManager.clearCaches()
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun getInstance(): ZepDepRootsResolverService = service()
  }
}