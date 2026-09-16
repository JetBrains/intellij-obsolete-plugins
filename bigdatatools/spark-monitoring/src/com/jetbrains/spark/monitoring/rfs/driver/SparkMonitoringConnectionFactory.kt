package com.jetbrains.spark.monitoring.rfs.driver

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataEx
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionFactory
import com.jetbrains.spark.monitoring.settings.SparkConnectionGroup

class SparkMonitoringConnectionFactory : RfsConnectionFactory {
  override fun create(
    groupId: BdtConnectionType,
    project: Project?,
    name: String,
    sshConfig: String?,
    url: String,
    additionalData: Map<String, String>,
    sourceConnection: String
  ): ConnectionDataEx? {
    if (groupId != BdtConnectionType.SPARK_MONITORING)
      return null

    val data = SparkConnectionGroup().createBlankData(perProject = project != null)
    data.uri = url
    data.name = name
    data.sourceConnection = sourceConnection
    sshConfig?.let {
      data.setTunnelData(ConnectionSshTunnelData(isEnabled = true, configId = it))
    }


    return data
  }
}