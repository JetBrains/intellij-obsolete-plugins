package com.intellij.bigdatatools.databricks.rfs.node

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.icon
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.isServerRunList
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.isWorkflowRunList
import com.intellij.bigdatatools.databricks.toolwindow.DatabricksMonitoringToolWindowController
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringRfsTreeNode
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import javax.swing.Icon

internal class DatabricksFileRfsTreeNode(
  project: Project,
  rfsPath: RfsPath,
  driver: DatabricksDriver,
) : MonitoringRfsTreeNode(project, rfsPath, driver) {
  init {
    myName = rfsPath.name
  }

  override fun isAlwaysLeaf() = rfsPath.isFile

  override fun onDoubleClick(): Boolean {
    val project = project ?: return true
    val controller = driver.getController(project) as? DatabricksMonitoringToolWindowController
    controller?.focusOn(focusId, rfsPath)
    return true
  }

  override fun name(): String {
    val databricksDataManager = driver.dataManager as DatabricksDataManager
    return when {
             rfsPath.parent?.isWorkflowRunList == true -> {
               val workflowRun = databricksDataManager.getWorkflowRun(rfsPath.name.toLong())
               workflowRun?.let { "${it.fileName} [${it.formatedStartTime}]" }
             }
             rfsPath.parent?.isServerRunList == true -> {
               val serverRun = databricksDataManager.getServerRun(rfsPath.name)
               serverRun?.let { "${it.fileName} [${it.formatedStartTime}]" }
             }
             else -> super.name()
           } ?: super.name()
  }

  override fun getIdleIcon(): Icon? {
    val dataManager = driver.dataManager as DatabricksDataManager
    return when {
      rfsPath.isRoot -> super.getIdleIcon()
      rfsPath.parent?.isServerRunList == true -> {
        dataManager.getServerRun(rfsPath.name)?.status?.icon
      }
      rfsPath.parent?.isWorkflowRunList == true -> {
        dataManager.getWorkflowRun(rfsPath.name.toLong())?.status?.icon
      }
      else -> null
    }
  }
}