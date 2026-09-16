package com.jetbrains.bigdatatools.glue.rfs

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataEx
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataExImpl
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionFactory
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionGroup

class GlueConnectionFactory : RfsConnectionFactory {
  override fun create(
    groupId: BdtConnectionType,
    project: Project?,
    name: String,
    sshConfig: String?,
    url: String,
    additionalData: Map<String, String>,
    sourceConnection: String
  ): ConnectionDataEx? {
    if (groupId != BdtConnectionType.GLUE)
      return null

    val data = GlueConnectionGroup().createBlankData(perProject = project != null)
    data.uri = url
    data.name = name
    data.sourceConnection = name
    sshConfig?.let {
      data.setTunnelData(ConnectionSshTunnelData(isEnabled = true, configId = it))
    }


    return data
  }
}