package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterDriver
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder
import javax.swing.Icon

class ArbitraryClusterDriver(override val connectionData: ArbitraryClusterConnectionData,
                             project: Project?,
                             testConnection: Boolean) : MonitoringDriver(project, testConnection), MasterDriver {
  override val icon: Icon = BigdatatoolsPluginSparkIcons.ArbitraryCluster

  override val dataManager = ArbitraryClusterDataManager(project, connectionData, ArbitraryClusterToolWindowSettings.getInstance())

  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = object : RfsDriverTreeNodeBuilder() {
    override fun createNode(project: Project, path: RfsPath, driver: Driver) =
      if (path.isCluster)
        ArbitraryClusterRfsTreeNode(project, path, this@ArbitraryClusterDriver, isCompound = true)
      else
        ArbitraryClusterRfsTreeNode(project, path, this@ArbitraryClusterDriver, isCompound = false)
  }

  init {
    Disposer.register(this, dataManager)
  }

  override fun getController(project: Project) = null

  override fun dispose() {}

  override fun doLoadChildren(rfsPath: RfsPath) = when {
    rfsPath.isRoot -> {
      val slaveConnections = connectionData.getSlaveConnections()
      val createdTypes = slaveConnections.map { it.connectionType }.toSet()
      val availableTypes = setOf(BdtConnectionType.SFTP, BdtConnectionType.SPARK_MONITORING)
      val stubTypes = availableTypes - createdTypes
      stubTypes.map {
        ArbitraryClusterFileInfo(this, it)
      }
    }
    else -> emptyList()
  }

  override fun prepareRefreshDependedDriver(driver: Driver) {}

  override fun listDependConnections(rfsPath: RfsPath): List<String> {
    return if (rfsPath.isRoot)
      connectionData.getSlaveConnections().map { it.connectionId }
    else
      emptyList()
  }

  override fun getDependConnectionRfsPath(connectionId: String) = root

  companion object {
    val RfsPath.isCluster
      get() = size == 0
  }
}