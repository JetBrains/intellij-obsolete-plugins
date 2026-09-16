package com.intellij.bigdatatools.databricks.rfs

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.bigdatatools.databricks.rfs.node.DatabricksFileRfsTreeNode
import com.intellij.bigdatatools.databricks.rfs.workspace.DatabricksWorkspaceDriver
import com.intellij.bigdatatools.databricks.toolwindow.DatabricksMonitoringToolWindowController
import com.intellij.bigdatatools.databricks.toolwindow.config.DatabricksToolWindowSettings
import com.intellij.bigdatatools.databricks.toolwindow.controllers.DatabricksGroupType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.DriverRfsTreeModel
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder
import javax.swing.Icon

internal class DatabricksDriver(override val connectionData: DatabricksConnectionData,
                       project: Project?,
                       testConnection: Boolean) : MonitoringDriver(project, testConnection) {
  override val dataManager: DatabricksDataManager = DatabricksDataManager(project, connectionData,
                                                                          DatabricksToolWindowSettings.getInstance())
  override val presentableName: String = connectionData.name
  override val icon: Icon = BigdatatoolsDatabricksIcons.Databricks


  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = object : RfsDriverTreeNodeBuilder() {
    override fun createNode(project: Project,
                            path: RfsPath,
                            driver: Driver) = DatabricksFileRfsTreeNode(project, path, this@DatabricksDriver)
  }

  val workspaceDriver = DatabricksWorkspaceDriver(project, this).also { Disposer.register(this, it) }

  init {
    Disposer.register(this, dataManager)

    dataManager.configurationModel.addListener(object : DataModelListener {
      override fun onChanged() {
        fileInfoManager.refreshFiles(confPath)
      }
    })
  }

  override fun dispose() {}

  override fun createTreeModel(rootPath: RfsPath, project: Project) = DriverRfsTreeModel(project, rootPath, this, false)

  override fun doLoadFileInfo(rfsPath: RfsPath) = DatabricksFileInfo(this, rfsPath)

  override fun doLoadChildren(rfsPath: RfsPath): List<FileInfo>? {
    dataManager.client.connectionError?.let {
      throw it
    }

    val children = when {
      rfsPath.isRoot -> listOfNotNull(confPath,
                                      workflowRunPath,
                                      serverRunPath)
      rfsPath.isConfiguration -> {
        null
      }
      rfsPath.isWorkflowRunList -> {
        dataManager.workflowExecutionStorage.getInfos().map { workflowRunPath.child(it.runId.toString(), false) }
      }
      rfsPath.isServerRunList -> {
        val infos = dataManager.serverExecutionStorage.getInfos()
        infos.map { serverRunPath.child(it.contextId, false) }
      }
      else -> null
    }
    return children?.map { DatabricksFileInfo(this, it) }
  }


  override fun getController(project: Project) = DatabricksMonitoringToolWindowController.getInstance(project)


  companion object {
    val confPath = RfsPath(listOf(DatabricksGroupType.CONF.title), false)
    val workflowRunPath = RfsPath(listOf(DatabricksGroupType.WORKFLOW_LIST.title), true)
    val serverRunPath = RfsPath(listOf(DatabricksGroupType.SERVER_RUNS_LIST.title), true)

    val RfsPath.isConfiguration
      get() = this == confPath
    val RfsPath.isWorkflowRunList
      get() = this == workflowRunPath
    val RfsPath.isServerRunList
      get() = this == serverRunPath
  }
}