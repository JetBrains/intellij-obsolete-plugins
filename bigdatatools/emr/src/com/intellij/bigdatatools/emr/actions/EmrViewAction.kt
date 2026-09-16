package com.intellij.bigdatatools.emr.actions

import com.intellij.bigdatatools.emr.rfs.EmrDriver.Companion.isCluster
import com.intellij.bigdatatools.emr.rfs.EmrFileInfo
import com.intellij.bigdatatools.emr.toolwindow.EmrToolWindowController
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.projectview.actions.RfsProjectPaneActionBase

class EmrViewAction : RfsProjectPaneActionBase() {

  override fun actionPerformed(e: AnActionEvent): Unit = withRfsPane(e) {
    val fileInfo = getSelectedFileInfo() ?: return
    val driver = getSelectedDriver() as? MonitoringDriver ?: return
    val controller = driver.getController(project) as? EmrToolWindowController
    controller?.focusOn(driver.connectionData.innerId, fileInfo.path.name)
  }

  override fun update(e: AnActionEvent): Unit = withRfsPane(e) {
    val isAppStub = getSelectedFileInfo()?.path?.parent?.isCluster == true
    val hasViewableFile = !isAppStub && isSingleDriverSelect() && getSelectedFileInfo() is EmrFileInfo
    e.presentation.isVisible = hasViewableFile
    e.presentation.isEnabled = hasViewableFile && isLoaded()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}