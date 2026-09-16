package com.jetbrains.spark.submit.run.cluster.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsProjectPaneActionBase
import com.jetbrains.bigdatatools.common.settings.actions.CreateConnectionPopup
import com.jetbrains.bigdatatools.common.settings.actions.showForToolbarOrInBestPositionFor
import com.jetbrains.bigdatatools.common.settings.paneadd.StandaloneCreateConnectionUtil
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider

class CreateClusterConnectionAction : RfsProjectPaneActionBase() {
  override fun actionPerformed(e: AnActionEvent) = withRfsPane(e) {
    CreateConnectionPopup.createPopup(StandaloneCreateConnectionUtil.createRootAddAction(project, RemoteTargetProvider.getConnectionGroups()), e).showForToolbarOrInBestPositionFor(e)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) = withRfsPane(e) {
    e.presentation.isEnabledAndVisible = true
  }
}