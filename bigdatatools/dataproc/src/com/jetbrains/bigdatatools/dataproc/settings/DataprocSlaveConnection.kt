package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtSlaveConnection
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo

data class DataprocSlaveConnection(override val connectionId: String = "",
                                   override val connectionType: BdtConnectionType,
                                   val byComponentGateway: Boolean = true,
                                   override val clusterId: String = "",
                                   val instanceName: String = "",
                                   val zone: String = "") : BdtSlaveConnection {
  companion object {
    fun createFor(cluster: DataprocClusterInfo,
                  appInfo: DataprocWebInterfaceInfo,
                  connection: ConnectionData) = DataprocSlaveConnection(connectionId = connection.innerId,
                                                                            connectionType = appInfo.connType!!,
                                                                            byComponentGateway = appInfo.componentGateway,
                                                                            clusterId = cluster.id,
                                                                            instanceName = cluster.name + "-m", zone = cluster.zone)
  }
}