package com.intellij.bigdatatools.databricks.toolwindow.controllers.workflow

import com.intellij.bigdatatools.databricks.toolwindow.config.DatabricksToolWindowSettings
import com.intellij.openapi.actionSystem.AnActionEvent

class WorkflowToggleCodeAction : WorkflowToggleBaseAction() {
  override fun isSelected(e: AnActionEvent): Boolean {
    return !DatabricksToolWindowSettings.getInstance().showCodeInResults
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val browser = e.getData(WorkflowDetailsController.WORKFLOW_DETAILS_BROWSER_KEY) ?: return
    DatabricksToolWindowSettings.getInstance().showCodeInResults = !state
    browser.cefBrowser.executeJavaScript("window.intelliJDatabricksTools.toggleCodeCells(${!state})", "", 0)
  }
}