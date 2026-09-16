package com.intellij.bigdatatools.zeppelin.cache

import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibraryType
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.annotations.TestOnly

class ZeppelinDependencyCacheManager {
  fun getSavedRoots(library: ZeppelinLibrary, project: Project?, configId: String): List<OrderRoot>? {
    val cachedClassesRoots = getCachedRootsByType(library, OrderRootType.CLASSES, project, configId)
    if (cachedClassesRoots != null) {
      val sourcesRoots = getCachedRootsByType(library, OrderRootType.SOURCES, project, configId) ?: emptyList()
      return cachedClassesRoots + sourcesRoots
    }
    return null
  }

  fun saveValues(library: ZeppelinLibrary, orderRoots: List<OrderRoot>, project: Project?, configId: String) {
    val (classesRoots, sourcesRoots) = orderRoots.partition { it.type == OrderRootType.CLASSES }

    cacheValuesByOrderType(library, classesRoots, OrderRootType.CLASSES, project, configId)
    cacheValuesByOrderType(library, sourcesRoots, OrderRootType.SOURCES, project, configId)
  }

  fun clearCachesForId(project: Project?, id: String) = getStorage(project).clearForId(id)

  private fun getCachedRootsByType(lib: ZeppelinLibrary, rootType: OrderRootType, project: Project?, configId: String): List<OrderRoot>? {
    if (!lib.dep.isMaven())
      return null

    val name = getStorageKey(lib.dep, rootType)
    val storage = getStorage(project)
    val cachedPaths = when (lib.type) {
                        ZeppelinLibraryType.INTERPRETER, ZeppelinLibraryType.USER -> {
                          storage.getConnectionDepsPaths(configId, name) ?: tryToGetConnectionPathsFromAnotherCache(configId, name, project)
                        }
                        ZeppelinLibraryType.BUILTIN -> {
                          storage.getGlobalLibraryPaths(name) ?: tryGetGlobalPathFromAnotherCache(name, project)
                        }
                      } ?: return null

    val fileManager = VirtualFileManager.getInstance()
    val virtualFiles = cachedPaths.map { url ->
      fileManager.refreshAndFindFileByUrl(url) ?: return null
    }

    return virtualFiles.map { OrderRoot(it, rootType) }
  }

  @TestOnly
  fun clearCaches() {
    getStorage(null).clearAll()
    ProjectManager.getInstance().openProjects.forEach {
      getStorage(it).clearAll()
    }
  }

  private fun cacheValuesByOrderType(library: ZeppelinLibrary,
                                     orderRoots: List<OrderRoot>,
                                     rootType: OrderRootType,
                                     project: Project?,
                                     configId: String) {
    val dep = library.dep
    if (!dep.isMaven())
      return

    val urls = orderRoots.map {
      it.file.url
    }
    val name = getStorageKey(dep, rootType)
    val storage = getStorage(project)

    when (library.type) {
      ZeppelinLibraryType.BUILTIN -> storage.saveGlobalLibraryPaths(name, urls)
      ZeppelinLibraryType.INTERPRETER, ZeppelinLibraryType.USER -> storage.saveInterpreterLibraryPaths(configId, name, urls)
    }
  }

  private fun getStorage(project: Project?): DependenciesPathStorage = if (project != null)
    ProjectDependenciesPathStorage.getInstance(project)
  else
    GlobalDependenciesPathStorage.getInstance()

  private fun tryGetGlobalPathFromAnotherCache(libName: String, project: Project?): List<String>? {
    val invertedStorage = getInvertedStorage(project)
    val paths = invertedStorage.getGlobalLibraryPaths(libName) ?: return null

    getStorage(project).saveGlobalLibraryPaths(libName, paths)

    return paths
  }

  private fun tryToGetConnectionPathsFromAnotherCache(configId: String, libName: String, project: Project?): List<String>? {
    val invertedStorage = getInvertedStorage(project)

    val paths = invertedStorage.getConnectionDepsPaths(configId, libName) ?: return null

    getStorage(project).saveInterpreterLibraryPaths(configId, libName, paths)

    return paths
  }


  private fun getInvertedStorage(project: Project?) = if (project != null) {
    getStorage(null)
  }
  else {
    getStorage(ProjectManager.getInstance().defaultProject)
  }

  private fun getStorageKey(dep: ZepDependency, rootType: OrderRootType) =
    "${dep.groupArtifactVersion}_${dep.exclusions.hashCode()}_${rootType.name()}"
}