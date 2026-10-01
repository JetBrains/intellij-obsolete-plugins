package com.intellij.dataWrangler.impl

import com.intellij.dataWrangler.impl.service.DataWranglerService
import com.intellij.dataWrangler.impl.view.DWMainPanel
import com.intellij.database.datagrid.DataGrid
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service

object DWMainPanelFactory {
  fun createMainPanel(grid: DataGrid, e: AnActionEvent): DWMainPanel? = createMainPanel(grid, e.dataContext, true)

  fun createMainPanel(grid: DataGrid, dataContext: DataContext, showAfterCreation: Boolean): DWMainPanel? {
    val session = grid.project.service<DataWranglerService>().createDWSession(dataContext) ?: return null
    return DWMainPanel(grid, session, showAfterCreation)
  }
}