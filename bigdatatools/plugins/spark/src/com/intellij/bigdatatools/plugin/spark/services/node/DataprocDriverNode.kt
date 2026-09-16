package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.dataproc.icons.BigdatatoolsDataprocIcons
import com.intellij.execution.services.ServiceViewLazyContributor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.settings.DataprocSlaveConnection
import com.jetbrains.bigdatatools.dataproc.toolwindow.controllers.DataprocClusterListController
import javax.swing.Icon
import javax.swing.JComponent

class DataprocDriverNode(project: Project,
                         parent: BdtBaseNode,
                         connectionData: ConnectionData,
                         override val label: String = connectionData.name) : BdtDriverNode(parent, project, connectionData),
                                                                             ServiceViewLazyContributor {
  override val icon: Icon = BigdatatoolsDataprocIcons.Dataproc
  private val dataManager: DataprocDataManager?
    get() = driver?.dataManager as? DataprocDataManager

  private val clusterListChangesListener = object : DataModelListener {
    override fun onChangedNonEdt() = refreshChildren()
    override fun onError(msg: String, e: Throwable?) = refreshChildren()
  }

  init {
    dataManager?.allClusterModel?.addListener(clusterListChangesListener)
  }

  override fun createContentController(): JComponent {
    if (!connData.isEnabled)
      return createConnectionDisabledPresentation()

    val dataManager = driver?.dataManager as? DataprocDataManager ?: return createErrorPresentation(
      MessagesBundle.message("services.panel.connection.error.unexpected"))
    val controller = DataprocClusterListController(project, dataManager, showDetails = false)

    return controller.getComponent()
  }


  override fun dispose() {
    super.dispose()
    dataManager?.allClusterModel?.removeListener(clusterListChangesListener)
  }


  @Suppress("UNCHECKED_CAST")
  override fun getChildren(): List<BdtDriverNode> {
    val clusterSummaries = dataManager?.allClusterModel?.data?.filter { !it.isStopped } ?: emptyList()

    val slaveConnections = (connData as MasterConnectionData<DataprocSlaveConnection>).getSlaveConnections()
      .filter { it.connectionType == BdtConnectionType.SPARK_MONITORING }


    val nodes = clusterSummaries.map { clusterInfo ->
      val notInitedNode = DataprocNotInitedSparkNode(project, this, connData, clusterInfo)
      val slaveConnection = slaveConnections.find { it.clusterId == clusterInfo.id } ?: return@map notInitedNode
      val connectionData = RfsConnectionDataManager.instance?.getConnectionById(project, slaveConnection.connectionId)
                           ?: return@map notInitedNode
      SparkMonitoringDriverNode(project, this, connectionData, clusterInfo.name, clusterInfo.icon)
    }
    nodes.forEach {
      if (isDisposed.get())
        return@forEach
      Disposer.register(this, it)
    }

    return nodes
  }
}