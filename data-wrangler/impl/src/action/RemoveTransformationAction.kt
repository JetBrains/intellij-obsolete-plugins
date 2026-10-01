package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.view.steps.TransformationStepsPanel.Companion.SELECTED_DW_STEP
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class RemoveTransformationAction : AnAction(), DumbAware {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val session = e.getData(DW_SESSION) ?: return
    val selectedElement = e.getData(SELECTED_DW_STEP) ?: return
    val transformationManager = session.getTransformationStepsManager()
    transformationManager.removeTransformation(selectedElement)
    session.rerunSession()
  }

  override fun update(e: AnActionEvent) {
    val session = e.getData(DW_SESSION)
    val selectedElement = e.getData(SELECTED_DW_STEP)
    e.presentation.isEnabledAndVisible = !isInitialCommand(session, selectedElement)
  }

  fun <C : DataWranglerContext> isInitialCommand(session: DataWranglerSession<C>?, step: TransformationStep<*, out DataWranglerContext>?): Boolean {
    if (session == null || step == null) return false
    return session.getEngine().getInitialStep(session.getContext()) != null && session.getTransformationStepsManager().findIndexByStep(step) == 0
  }
}