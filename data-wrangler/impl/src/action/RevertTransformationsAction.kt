package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.impl.view.steps.TransformationStepsPanel.Companion.SELECTED_DW_STEP
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class RevertTransformationsAction : AnAction(), DumbAware {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val session = e.getData(DW_SESSION) ?: return
    val selectedElement = e.getData(SELECTED_DW_STEP) ?: return
    val transformationManager = session.getTransformationStepsManager()
    transformationManager.resetTransformationsTo(selectedElement)
    session.rerunSession()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(DW_SESSION) != null && e.getData(SELECTED_DW_STEP) != null
  }
}