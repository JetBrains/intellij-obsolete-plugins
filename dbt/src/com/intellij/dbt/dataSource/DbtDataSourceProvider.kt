package com.intellij.dbt.dataSource

import com.intellij.database.dataSource.rawDataSource
import com.intellij.database.psi.DataSourceManager
import com.intellij.database.psi.DbDataSource
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.VirtualFileDataSourceProvider
import com.intellij.dbt.dbtSettings
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge

class DbtDataSourceProvider : VirtualFileDataSourceProvider() {
  override fun getDataSource(project: Project, virtualFile: VirtualFile): DbDataSource? {
    val module = ModuleUtilCore.findModuleForFile(virtualFile, project) ?: return null
    val dbtSettings = (module as? ModuleBridge)?.findModuleEntity(module.entityStorage.current)?.dbtSettings ?: return null

    if (dbtSettings.dbtDataSourceId?.isNotEmpty() == true) {
      val dataSources = DataSourceManager.getManagers(project).flatMap { it.dataSources }
      val dataSource = dataSources.firstOrNull { it.rawDataSource?.uniqueId == dbtSettings.dbtDataSourceId } ?: return null
      return DbPsiFacade.getInstance(project).findDataSource(dataSource.uniqueId)
    }
    return null
  }
}