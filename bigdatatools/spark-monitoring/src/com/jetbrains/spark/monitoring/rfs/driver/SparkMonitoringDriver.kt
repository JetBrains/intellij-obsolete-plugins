package com.jetbrains.spark.monitoring.rfs.driver

import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.ApplicationsStartedFromIdeRegistry
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.tree.DriverRfsTreeModel
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.settings.SparkConnectionData

class SparkMonitoringDriver(override val connectionData: SparkConnectionData,
                            project: Project?,
                            testConnection: Boolean) : MonitoringDriver(project, testConnection) {
  override val dataManager = SparkDataManager(connectionData, project)
  override val icon = BigdatatoolsSparkMonitoringIcons.Spark

  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = object : RfsDriverTreeNodeBuilder() {
    override fun createNode(project: Project, path: RfsPath, driver: Driver) =
      SparkRfsTreeNode(project, path, this@SparkMonitoringDriver)
  }

  init {
    Disposer.register(this, dataManager)
    dataManager.applications.addListener(object : DataModelListener {
      override fun onChanged() {
        fileInfoManager.refreshFiles(root)
      }
    })
  }

  override fun dispose() {}

  override fun createTreeModel(rootPath: RfsPath, project: Project) = DriverRfsTreeModel(project, rootPath, this, false)

  override fun doLoadFileInfo(rfsPath: RfsPath) = SparkFileInfo(this, rfsPath, null, false)

  override fun doLoadChildren(rfsPath: RfsPath): List<FileInfo> {
    dataManager.client.connectionError?.let {
      throw it
    }

    return when {
      rfsPath.isRoot -> {
        dataManager.applications.error?.let { throw it }
        dataManager.applications.data?.map {
          val isOurTask = serviceOrNull<ApplicationsStartedFromIdeRegistry>()?.getSourceConfigurationId(project, it.idString,
                                                                                                        connectionData) != null
          SparkFileInfo(this, rfsPath.child(it.idString, false), it, isOurTask)
        } ?: emptyList()
      }
      else -> emptyList()
    }
  }

  override fun getController(project: Project) = null

}