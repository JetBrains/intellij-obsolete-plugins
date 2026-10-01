package com.intellij.dbt.detection

import com.intellij.dbt.DbtEntitySource
import com.intellij.dbt.DbtModuleEntity
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.DbtUtils.Companion.guessDbtExecutable
import com.intellij.dbt.dbtSettings
import com.intellij.dbt.fus.DbtProjectCollector
import com.intellij.dbt.modifyDbtModuleEntity
import com.intellij.dbt.run.DbtRunConfigurationFactory
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.sql.dialects.generic.GenericDialect
import com.intellij.sql.psi.SqlPsiFacade
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class DbtService(private val project: Project, val coroutineScope: CoroutineScope) {
  suspend fun initializeDbtSupport(module: Module, dbtDirectory: VirtualFile) {
    val moduleBridge = module as? ModuleBridge ?: return
    val moduleEntity = moduleBridge.findModuleEntity(moduleBridge.entityStorage.current) ?: return
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    val guessedDbtExecutablePath = guessDbtExecutable(module)

    withContext(Dispatchers.EDT) {
      SqlPsiFacade.getInstance(project).setDialectMapping(dbtDirectory, GenericDialect.INSTANCE)
    }
    edtWriteAction {
      WorkspaceModel.getInstance(project).updateProjectModel("Add dbt settings") { builder ->
        builder.modifyModuleEntity(moduleEntity) {
          this.dbtSettings = DbtModuleEntity(false, DbtEntitySource) {
            dbtProjectPath = dbtDirectory.toVirtualFileUrl(virtualFileUrlManager)
            dbtExecutablePath = guessedDbtExecutablePath
          }
        }
      }
    }

    DbtProjectCollector.logDbtProjectInitialized()
    DbtRunConfigurationFactory.addTopCommandDbtRunConfigurations(module)
  }

  suspend fun removeDbtSupport(module: Module) {
    val dbtSettings = DbtUtils.getDbtSettings(module) ?: return
    edtWriteAction {
      WorkspaceModel.getInstance(project).updateProjectModel("Update dbt settings") { builder ->
        builder.removeEntity(dbtSettings)
      }
    }
  }

  suspend fun processModule(module: Module) {
    val moduleBridge = module as? ModuleBridge ?: return
    val moduleEntity = moduleBridge.findModuleEntity(moduleBridge.entityStorage.current) ?: return
    var dbtDirectoryFound: VirtualFile? = null
    withContext(Dispatchers.IO + CoroutineName("DbtProjectActivity")) {
      readAction {
        dbtDirectoryFound = findDbtDirectory(module)
      }
    }
    val dbtDirectory = dbtDirectoryFound

    val dbtSettings = moduleEntity.dbtSettings
    if (dbtSettings == null) {
      if (dbtDirectory != null) {
        initializeDbtSupport(module, dbtDirectory)
      }
    }
    else {
      if (dbtDirectory == null) {
        removeDbtSupport(module)
      }
      else {
        updateDbtDirectory(dbtDirectory, dbtSettings)
      }
    }
  }

  private suspend fun updateDbtDirectory(dbtDirectory: VirtualFile,
                                         dbtSettings: DbtModuleEntity) {
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    withContext(Dispatchers.EDT) {
      SqlPsiFacade.getInstance(project).setDialectMapping(dbtDirectory, GenericDialect.INSTANCE)
    }
    edtWriteAction {
      WorkspaceModel.getInstance(project).updateProjectModel("Update dbt settings") { builder ->
        builder.modifyDbtModuleEntity(dbtSettings) {
          dbtProjectPath = dbtDirectory.toVirtualFileUrl(virtualFileUrlManager)
        }
      }
    }
  }

  @RequiresBackgroundThread
  private fun findDbtDirectory(module: Module): VirtualFile? {
    if (module.isDisposed) return null
    return DbtUtils.findDbtDirectory(module)
  }

  companion object {
    fun getInstance(project: Project): DbtService = project.service()
  }
}