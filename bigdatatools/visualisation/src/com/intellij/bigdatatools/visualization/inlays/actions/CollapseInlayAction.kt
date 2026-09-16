package com.intellij.bigdatatools.visualization.inlays.actions

import com.intellij.bigdatatools.visualization.BigdatatoolsVisualisationIcons
import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponent.Companion.NOTEBOOK_INLAY_COMPONENT_KEY
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class CollapseInlayAction : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT // reads mutable fields

  override fun update(e: AnActionEvent) {
    val inlayComponent = e.getData(NOTEBOOK_INLAY_COMPONENT_KEY)
    e.presentation.isEnabledAndVisible = inlayComponent != null && inlayComponent.collapsible
    if (inlayComponent != null) {
      e.presentation.icon = if (inlayComponent.collapsed) BigdatatoolsVisualisationIcons.Unfold else BigdatatoolsVisualisationIcons.Fold
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val inlayComponent = e.getData(NOTEBOOK_INLAY_COMPONENT_KEY) ?: return
    inlayComponent.onCollapseExpand()
  }
}