package com.intellij.bigdatatools.emr.actions

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.emr.rfs.EmrDriver.Companion.isCluster
import com.intellij.bigdatatools.emr.rfs.EmrFileInfo
import com.intellij.bigdatatools.emr.rfs.EmrRfsTreeNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsProjectPaneActionBase

class EmrCreateDependAction : RfsProjectPaneActionBase() {
  override fun actionPerformed(e: AnActionEvent) = withRfsPane(e) {
    val emrRfsTreeNode = getSelectedDriverNode() as? EmrRfsTreeNode ?: return@withRfsPane
    emrRfsTreeNode.onDoubleClick()
  }

  override fun update(e: AnActionEvent): Unit = withRfsPane(e) {
    val selectedFileInfo = getSelectedFileInfo() as? EmrFileInfo ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val isAppStub = selectedFileInfo.path.parent?.isCluster == true
    e.presentation.isEnabledAndVisible = isAppStub
    e.presentation.text = MessagesBundle.message("rfs.action.create.depend.title", selectedFileInfo.path.name)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}