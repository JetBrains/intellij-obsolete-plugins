package com.intellij.bigdatatools.databricks.toolwindow.controllers.workflow

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

abstract class WorkflowToggleBaseAction : DumbAwareToggleAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    super.update(e)
    val workflowDetailsController = e.getData(WorkflowDetailsController.WORKFLOW_DETAILS_CONTROLLER_KEY)
    val browser = e.getData(WorkflowDetailsController.WORKFLOW_DETAILS_BROWSER_KEY)
    e.presentation.isEnabledAndVisible = workflowDetailsController != null && browser != null &&
                                         workflowDetailsController.contentType == WorkflowDetailsController.ContentType.HTML
  }
}