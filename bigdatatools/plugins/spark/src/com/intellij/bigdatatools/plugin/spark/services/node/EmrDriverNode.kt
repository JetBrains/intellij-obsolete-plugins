package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrSlaveConnection
import com.intellij.bigdatatools.emr.rfs.EmrRfsTreeNode
import com.intellij.bigdatatools.emr.settings.EmrConnectionGroup
import com.intellij.bigdatatools.emr.toolwindow.controllers.EmrClusterController
import com.intellij.execution.services.ServiceViewLazyContributor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import javax.swing.Icon
import javax.swing.JComponent

class EmrDriverNode(project: Project,
                    parent: BdtBaseNode,
                    connectionData: ConnectionData,
                    override val label: String = connectionData.name) : BdtDriverNode(parent, project, connectionData),
                                                                        ServiceViewLazyContributor {
  override val icon: Icon = EmrConnectionGroup.ICON

  private val clusterListChangesListener = object : DataModelListener {
    override fun onChangedNonEdt() = refreshChildren()
    override fun onError(msg: String, e: Throwable?) = refreshChildren()
  }

  init {
    (driver?.dataManager as? EmrDataManager)?.clusterModel?.addListener(clusterListChangesListener)
  }

  override fun dispose() {
    super.dispose()
    (driver?.dataManager as? EmrDataManager)?.clusterModel?.removeListener(clusterListChangesListener)
  }

  override fun createContentController(): JComponent {
    if (!connData.isEnabled)
      return createConnectionDisabledPresentation()

    val dataManager = driver?.dataManager as? EmrDataManager ?: return createErrorPresentation(
      MessagesBundle.message("services.panel.connection.error.unexpected"))
    val controller = EmrClusterController(project, dataManager, showDetails = false)
    Disposer.register(this, controller)
    return controller.getComponent()
  }


  @Suppress("UNCHECKED_CAST")
  override fun getChildren(): List<BdtDriverNode> {
    val emrDataManager = driver?.dataManager as? EmrDataManager ?: return emptyList()
    val clusterSummaries = emrDataManager.clusterModel.data?.filter { !it.isStopped } ?: emptyList()

    val slaveConnections = (connData as MasterConnectionData<EmrSlaveConnection>).getSlaveConnections()
      .filter { it.connectionType == BdtConnectionType.SPARK_MONITORING }


    val nodes = clusterSummaries.map { clusterInfo ->
      val notInitedNode = EmrNotInitedSparkNode(project, this, connData, clusterInfo)
      val slaveConnection = slaveConnections.find { it.clusterId == clusterInfo.id } ?: return@map notInitedNode
      val connectionData = RfsConnectionDataManager.instance?.getConnectionById(project, slaveConnection.connectionId)
                           ?: return@map notInitedNode
      SparkMonitoringDriverNode(project, this, connectionData, clusterInfo.name, EmrRfsTreeNode.getIconForCluster(clusterInfo))
    }
    nodes.forEach {
      if (isDisposed.get())
        return@forEach
      Disposer.register(this, it)
    }

    return nodes
  }
}