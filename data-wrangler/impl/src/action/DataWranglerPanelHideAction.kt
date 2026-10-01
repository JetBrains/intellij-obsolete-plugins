package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.impl.view.findDWPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DataWranglerPanelHideAction : AnAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    setDataWranglerActionState(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    findDWPanel(e.dataContext)?.hidePanel()
  }
}