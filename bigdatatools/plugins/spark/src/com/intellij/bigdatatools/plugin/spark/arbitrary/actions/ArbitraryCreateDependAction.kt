package com.intellij.bigdatatools.plugin.spark.arbitrary.actions

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterFileInfo
import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterRfsTreeNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsProjectPaneActionBase

class ArbitraryCreateDependAction : RfsProjectPaneActionBase() {
  override fun actionPerformed(e: AnActionEvent) = withRfsPane(e) {
    val treeNode = getSelectedDriverNode() as? ArbitraryClusterRfsTreeNode ?: return@withRfsPane
    treeNode.onDoubleClick()
  }

  override fun update(e: AnActionEvent): Unit = withRfsPane(e) {
    val selectedFileInfo = getSelectedFileInfo() as? ArbitraryClusterFileInfo ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val isAppStub = selectedFileInfo.path.parent?.isRoot == true
    e.presentation.isEnabledAndVisible = isAppStub
    e.presentation.text = MessagesBundle.message("rfs.action.create.depend.title", selectedFileInfo.path.name)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}