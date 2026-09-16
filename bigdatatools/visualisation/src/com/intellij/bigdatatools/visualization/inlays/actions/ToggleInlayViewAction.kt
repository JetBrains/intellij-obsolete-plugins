package com.intellij.bigdatatools.visualization.inlays.actions

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponent
import com.intellij.bigdatatools.visualization.inlays.pages.InlaySplitPage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

internal abstract class ToggleInlayViewAction(private val mode: InlaySplitPage.Mode) : DumbAwareToggleAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    super.update(e)
    val inlayComponent = e.getData(NotebookInlayComponent.NOTEBOOK_INLAY_COMPONENT_KEY)
    val splitPage = e.getData(InlaySplitPage.NOTEBOOK_INLAY_SPLIT_PAGE_KEY)
    e.presentation.isEnabledAndVisible = inlayComponent != null && !inlayComponent.collapsed &&
                                         splitPage != null && splitPage.supportsMode(mode)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return e.getData(InlaySplitPage.NOTEBOOK_INLAY_SPLIT_PAGE_KEY)?.mode == mode
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    e.getData(InlaySplitPage.NOTEBOOK_INLAY_SPLIT_PAGE_KEY)?.switchMode(mode)
  }
}

internal class ToggleTableInlayViewAction : ToggleInlayViewAction(InlaySplitPage.Mode.TABLE)
internal class ToggleChartInlayViewAction : ToggleInlayViewAction(InlaySplitPage.Mode.CHART)
internal class ToggleSplitInlayViewAction : ToggleInlayViewAction(InlaySplitPage.Mode.SPLIT)
internal class ToggleConsoleInlayViewAction : ToggleInlayViewAction(InlaySplitPage.Mode.CONSOLE)
