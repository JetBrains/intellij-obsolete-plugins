package com.intellij.bigdatatools.zeppelin.dependency.module

import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.EmptyModuleType
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

object ZeppelinModuleUtils {
  fun getModule(project: Project, configId: String) = getAllModules(project).filter { !it.isDisposed }.firstOrNull {
    it.name == configId
  }

  fun initModule(project: Project, configId: String, parentDisposable: Disposable): Module {
    val module = getModule(project, configId) ?: createModule(project, configId)
    if (module.getUserData(CONFIG_ID_KEY) != null)
      return module

    module.putUserData(CONFIG_ID_KEY, configId)

    Disposer.register(parentDisposable, Disposable {
      deleteModule(module)
    })

    //We need to add Dispose with project, because project services are disposed AFTER dispose project
    //and module will not be removed, see BDIDE-2031
    @Suppress("IncorrectParentDisposable")
    Disposer.register(project, Disposable {
      deleteModule(module)
    })

    return module
  }

  private fun getModuleType() = try {
    JavaModuleType.getModuleType().id
  }
  catch (t: Throwable) {
    EmptyModuleType.getInstance().id
  }

  private fun createModule(project: Project, configId: String) = runWriteAction {
    val modifiableModel = ModuleManager.getInstance(project).getModifiableModel()
    val module = modifiableModel.newNonPersistentModule(configId, getModuleType())
    modifiableModel.commit()
    ModuleRootModificationUtil.setSdkInherited(module)

    module
  }

  private fun deleteModule(module: Module) = invokeAndWaitIfNeeded {
    runWriteAction {
      if (module.isDisposed)
        return@runWriteAction
      try {
        val moduleManager = ModuleManager.getInstance(module.project)
        val modifiableModuleModel = moduleManager.getModifiableModel()
        val moduleFile = module.moduleFile
        modifiableModuleModel.disposeModule(module)
        modifiableModuleModel.commit()
        moduleFile?.delete(this)
      }
      catch (t: Throwable) {
        logger.warn(t)
      }
    }
  }

  fun getModulesNonZeppelin(project: Project, ignoreToo: List<String>) = getAllModules(project)
    .filter { it.getUserData(CONFIG_ID_KEY) == null }
    .filter { it.name !in ignoreToo }

  fun getContainingZeppelinModule(virtualFile: VirtualFile, project: Project): Module? {
    val configId = NotebookFileUtil.getConfigId(virtualFile) ?: return null
    return getModule(project, configId)
  }

  private fun getAllModules(project: Project): List<Module> {
    val moduleManager = ModuleManager.getInstance(project)
    val allModules = moduleManager.modules

    return allModules.toList()
  }

  private val CONFIG_ID_KEY = Key<String>("CONFIG_ID")
  private val logger = Logger.getInstance(this::class.java)
}