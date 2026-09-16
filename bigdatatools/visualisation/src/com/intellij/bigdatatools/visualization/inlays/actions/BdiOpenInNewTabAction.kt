package com.intellij.bigdatatools.visualization.inlays.actions

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponent
import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.dataframe.DataFrameKeys
import com.intellij.charts.dataframe.DataFrameKeys.DATA_FRAME_DATA_KEY
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

class BdiOpenInNewTabAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val dataFrame = e.getData(DATA_FRAME_DATA_KEY)
    val project = e.project
    val inlay = e.getData(NotebookInlayComponent.NOTEBOOK_INLAY_COMPONENT_KEY)
    e.presentation.isEnabledAndVisible = dataFrame != null && project != null && inlay != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val dataFrame = e.getData(DATA_FRAME_DATA_KEY) ?: return
    val project = e.project ?: return
    val inlay = e.getData(NotebookInlayComponent.NOTEBOOK_INLAY_COMPONENT_KEY) ?: return
    openInNewTab(inlay, dataFrame, project)
  }
}

fun openInNewTab(inlay: NotebookInlayComponent, dataFrame: DataFrame, project: Project) {
  val cellTitle = if (inlay.cell.titleVisible && !inlay.cell.title.isNullOrBlank()) inlay.cell.title else "cell ${inlay.cell.indexInNote}"
  val file = LightVirtualFile("${inlay.editor.virtualFile!!.name.removeSuffix(".zpln")}.${cellTitle}").apply {
    putUserData(DataFrameKeys.DATA_FRAME, dataFrame)
  }

  FileEditorManager.getInstance(project).openFile(file, true)
}

