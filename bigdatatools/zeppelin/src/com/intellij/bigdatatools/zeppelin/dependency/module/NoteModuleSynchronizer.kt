package com.intellij.bigdatatools.zeppelin.dependency.module

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.bigdatatools.zeppelin.dependency.model.NoteDependency
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibraryType
import com.intellij.bigdatatools.zeppelin.dependency.resolver.CustomDependencyResolveProvider
import com.intellij.bigdatatools.zeppelin.dependency.resolver.ScalaSdkDependencyPatcher
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.jarRepository.RepositoryLibraryType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Disposer

object NoteModuleSynchronizer {
  fun createOrUpdateModuleWithDependencies(project: Project,
                                           configId: String,
                                           libs: List<ZeppelinLibrary>,
                                           roots: List<List<OrderRoot>>,
                                           modules: List<ZeppelinLibrary>,
                                           customDeps: List<NoteDependency>,
                                           parentDisposable: Disposable) = StartupManager.getInstance(project).runAfterOpened {
    @Suppress("DEPRECATION")
    if (Disposer.isDisposed(parentDisposable))
      return@runAfterOpened
    if (ApplicationManager.getApplication().isUnitTestMode)
      return@runAfterOpened

    val module = invokeAndWaitIfNeeded {
      @Suppress("DEPRECATION")
      if (Disposer.isDisposed(parentDisposable))
        return@invokeAndWaitIfNeeded null

      ZeppelinModuleUtils.initModule(project, configId, parentDisposable)
    } ?: return@runAfterOpened

    if (roots.isNotEmpty())
      updateModuleDependencies(module, libs, roots, modules)

    CustomDependencyResolveProvider.getAll().forEach { it.runDependencyResolve(module, customDeps) }
  }


  fun isExists(project: Project, configId: String, zeppelinLibrary: ZeppelinLibrary): Boolean {
    val module = ZeppelinModuleUtils.getModule(project, configId) ?: return false
    val modifiableModel = ModuleRootManager.getInstance(module).modifiableModel
    try {
      if (zeppelinLibrary.dep.isModule()) {
        return ModuleManager.getInstance(project).findModuleByName(zeppelinLibrary.dep.moduleName) != null &&
               modifiableModel.moduleDependencies.any { it.name == zeppelinLibrary.dep.moduleName }
      }
      else {
        val libraryTable = modifiableModel.moduleLibraryTable
        val lib = libraryTable.getLibraryByName(zeppelinLibrary.presentableName) ?: return false
        return lib.getUrls(OrderRootType.CLASSES).isNotEmpty()
      }
    }
    finally {
      modifiableModel.dispose()
    }
  }

  private fun updateModuleDependencies(module: Module,
                                       libs: List<ZeppelinLibrary>,
                                       roots: List<List<OrderRoot>>,
                                       moduleLibs: List<ZeppelinLibrary>) = invokeAndWaitIfNeeded {
    withModuleModel(module) { modifiableModuleModel ->
      updateLibs(modifiableModuleModel, libs, roots)
      updateModules(moduleLibs.map { it.dep }, modifiableModuleModel)
    }
    return@invokeAndWaitIfNeeded module
  }

  private fun updateModules(moduleDependencies: List<ZepDependency>,
                            modifiableModuleModel: ModifiableRootModel) {
    modifiableModuleModel.moduleDependencies.mapNotNull {
      val order: ModuleOrderEntry = modifiableModuleModel.findModuleOrderEntry(it) ?: return@mapNotNull null
      modifiableModuleModel.removeOrderEntry(order)
    }
    moduleDependencies.forEach {
      val module = ModuleManager.getInstance(modifiableModuleModel.project).findModuleByName(it.moduleName) ?: return@forEach
      modifiableModuleModel.addModuleOrderEntry(module)
    }
  }

  private fun updateLibs(modifiableModuleModel: ModifiableRootModel,
                         zeppelinLibs: List<ZeppelinLibrary>,
                         libsRoots: List<List<OrderRoot>>) {
    val libTable = modifiableModuleModel.moduleLibraryTable
    val modifiableLibraryModel = libTable.modifiableModel

    modifiableLibraryModel.libraries.forEach {
      modifiableLibraryModel.removeLibrary(it)
    }

    zeppelinLibs.zip(libsRoots).forEach { (zeppelinLibrary, roots) ->
      if (roots.isNotEmpty())
        createLibWithRoots(zeppelinLibrary, modifiableLibraryModel, roots)
    }

    val notLoaded = zeppelinLibs.zip(libsRoots).mapNotNull { (zeppelinLibrary, roots) ->
      if (roots.isEmpty() && zeppelinLibrary.type == ZeppelinLibraryType.BUILTIN)
        zeppelinLibrary
      else
        null
    }

    if (notLoaded.isNotEmpty()) {
      invokeLater {
        val unresolvedDeps = notLoaded.joinToString { it.dep.groupArtifactVersion }
        val throwable = Throwable(NoteMessagesBundle.message("resolve.error.builtin", unresolvedDeps))
        NotificationUtils.notifyException(throwable, NoteMessagesBundle.message("resolve.error.builtin.title"))
      }
    }

    modifiableLibraryModel.commit()
  }

  private fun createLibWithRoots(zeppelinLibrary: ZeppelinLibrary,
                                 modifiableLibraryModel: LibraryTable.ModifiableModel,
                                 roots: List<OrderRoot>) {
    val dep = zeppelinLibrary.dep
    val type = if (dep.isMaven()) RepositoryLibraryType.REPOSITORY_LIBRARY_KIND else null
    val presentableName = zeppelinLibrary.presentableName

    ScalaSdkDependencyPatcher.getDependencyPatcher().setupLibrary(presentableName, roots, dep, modifiableLibraryModel, type)
  }

  private fun <T> withModuleModel(module: Module, body: (ModifiableRootModel) -> T): T? = invokeAndWaitIfNeeded {
    if (module.isDisposed) return@invokeAndWaitIfNeeded null
    val moduleModel: ModifiableRootModel = ModuleRootManager.getInstance(module).modifiableModel

    try {
      return@invokeAndWaitIfNeeded body(moduleModel)
    }
    finally {
      if (moduleModel.isDisposed || !moduleModel.isChanged)
        moduleModel.dispose()
      else runWriteAction {
        moduleModel.commit()
      }
    }
  }
}