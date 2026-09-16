package com.jetbrains.bigdatatools.hivemetastore.rfs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataEx
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionFactory
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionGroup

class HiveMetastoreConnectionFactory : RfsConnectionFactory {
  override fun create(
    groupId: BdtConnectionType,
    project: Project?,
    name: String,
    sshConfig: String?,
    url: String,
    additionalData: Map<String, String>,
    sourceConnection: String
  ): ConnectionDataEx? {
    if (groupId != BdtConnectionType.HIVE)
      return null

    val data = HiveMetastoreConnectionGroup().createBlankData(perProject = project != null)
    data.uri = url
    data.name = name
    data.sourceConnection = sourceConnection
    sshConfig?.let {
      data.setTunnelData(ConnectionSshTunnelData(isEnabled = true, configId = it))
    }


    return data
  }
}