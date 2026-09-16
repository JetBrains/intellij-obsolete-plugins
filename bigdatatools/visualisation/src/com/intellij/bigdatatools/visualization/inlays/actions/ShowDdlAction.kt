package com.intellij.bigdatatools.visualization.inlays.actions

import com.intellij.bigdatatools.visualization.inlays.components.DDLDialog
import com.intellij.charts.dataframe.DataFrameKeys.DATA_FRAME_DATA_KEY
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ShowDdlAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project != null && e.getData(DATA_FRAME_DATA_KEY) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val dataFrame = e.getData(DATA_FRAME_DATA_KEY) ?: return
    DDLDialog(project, dataFrame).show()
  }
}