package com.intellij.bigdatatools.emr.rfs

import com.intellij.bigdatatools.awsBase.ui.AwsUiUtil
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.settings.EmrConnectionData
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.toolwindow.EmrToolWindowController
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterDriver
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import org.com.jetbrains.bigdatatools.icons.Icons
import javax.swing.Icon

class EmrDriver(override val connectionData: EmrConnectionData,
                project: Project?, testConnection: Boolean) : MonitoringDriver(project, testConnection), MasterDriver {
  override val icon: Icon = Icons.EMR_ICON

  override val dataManager = EmrDataManager(project, connectionData, EmrToolWindowSettings.getInstance())

  override val presentableName: String = "${connectionData.name} [${AwsUiUtil.getAuthName(connectionData)}] [${connectionData.region}]"

  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = object : RfsDriverTreeNodeBuilder() {
    override fun createNode(project: Project, path: RfsPath, driver: Driver): EmrRfsTreeNode {
      val clusterName = if (path.size > 0) path.name(0) else ""
      return if (path.isCluster)
        EmrRfsTreeNode(project, path, dataManager.getClusterById(clusterName), this@EmrDriver, isCompound = true)
      else
        EmrRfsTreeNode(project, path, dataManager.getClusterById(clusterName), this@EmrDriver, isCompound = false)
    }
  }

  init {
    Disposer.register(this, dataManager)

    dataManager.clusterModel.addListener(object : DataModelListener {
      override fun onChanged() = executeOnPooledThread {
        this@EmrDriver.fileInfoManager.refreshFiles(root)
      }
    })
  }

  override val isRfsViewEditorAvailable = false

  override fun getMetaInfoProvider() = EmrFileMetaInfoProvider(this)

  override fun getController(project: Project) = EmrToolWindowController.getInstance(project)

  override fun dispose() {}

  override fun doLoadChildren(rfsPath: RfsPath): List<EmrFileInfo> {
    return when {
      rfsPath.isRoot -> dataManager.clusterModel.entries.map { EmrFileInfo(this, it, null) }
      rfsPath.isCluster -> {
        val clusterInfo = dataManager.getClusterById(rfsPath.name) ?: return emptyList()
        if (clusterInfo.isStopped)
          return emptyList()

        val slaveConnections = connectionData.getSlaveConnections().filter { it.clusterId == clusterInfo.id }
        val createdTypes = slaveConnections.map { it.connectionType }
        val defaultTypes = setOf(BdtConnectionType.SFTP, BdtConnectionType.SPARK_MONITORING)
        val apps = dataManager.getClusterDetailsOrLoad(clusterInfo.id).getApps()
        val availableTypes = apps.map { it.connType }.intersect(defaultTypes)
        val stubTypes = availableTypes - createdTypes
        stubTypes.map {
          EmrFileInfo(this, clusterInfo, it)
        }
      }
      else -> emptyList()
    }
  }

  override fun prepareRefreshDependedDriver(driver: Driver) {}

  override fun listDependConnections(rfsPath: RfsPath): List<String> {
    return connectionData.getSlaveConnections().filter { it.clusterId == rfsPath.name }.map { it.connectionId }
  }

  override fun getDependConnectionRfsPath(connectionId: String): RfsPath? {
    val clusterId = connectionData.getSlaveDriverConfig(connectionId)?.clusterId ?: return null
    return createRfsPath(clusterId.withSlash())
  }

  companion object {
    val RfsPath.isCluster: Boolean
      get() = size == 1
  }
}