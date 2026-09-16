package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtSlaveConnection

data class EmrSlaveConnection(override val connectionId: String = "",
                              override val connectionType: BdtConnectionType,
                              override val clusterId: String = "",
                              val url: String = "") : BdtSlaveConnection {
  companion object {
    fun createFor(cluster: EmrClusterDetails,
                  appInfo: EmrClusterAppInfo,
                  connection: ConnectionData) = EmrSlaveConnection(connectionId = connection.innerId,
                                                                       connectionType = appInfo.connType!!,
                                                                       clusterId = cluster.id,
                                                                       url = appInfo.url)
  }
}