package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.impl.DWMainPanelFactory
import com.intellij.dataWrangler.impl.fus.DataWranglerInputType
import com.intellij.dataWrangler.impl.fus.DataWranglerProviderCollector
import com.intellij.dataWrangler.impl.service.DataWranglerService
import com.intellij.dataWrangler.impl.view.findDWPanel
import com.intellij.database.datagrid.GridPanel.ViewPosition
import com.intellij.database.datagrid.GridUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.PlatformUtils

open class DataWranglerPanelToggleAction : ToggleAction(), DumbAware {

  init {
    templatePresentation.apply {
      putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      putClientProperty(ActionUtil.USE_SMALL_FONT_IN_TOOLBAR, true)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    setDataWranglerActionState(e)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    val dwPanel = findDWPanel(e.dataContext) ?: return false
    val grid = GridUtil.getDataGrid(e.dataContext) ?: return false
    return grid.panel.getSideView(ViewPosition.RIGHT) == dwPanel
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val grid = GridUtil.getDataGrid(e.dataContext)
    if (grid == null) return

    if (state) {
      var dwPanel = findDWPanel(e.dataContext)
      if (dwPanel == null) {
        dwPanel = DWMainPanelFactory.createMainPanel(grid, e) ?: return
        DataWranglerProviderCollector.logDWOpened(DataWranglerInputType.PYTHON_VARIABLE)
      }
      else {
        dwPanel.showPanel()
      }
    }
    else {
      grid.findDWPanel()?.hidePanel()
    }
  }
}

fun setDataWranglerActionState(e: AnActionEvent) {
  val grid = GridUtil.getDataGrid(e.dataContext)
  e.presentation.isVisible = Registry.`is`("tables.datawrangler.plugin.show.internal.action") && grid != null
  e.presentation.isEnabled = grid?.project?.service<DataWranglerService>()?.canCreateDWSession(e.dataContext) == true

  //We do want to show disabled actions only in the Pycharm
  if (PlatformUtils.isPyCharm()) {
    e.presentation.isEnabledAndVisible = e.presentation.isEnabled && e.presentation.isVisible
  }
}