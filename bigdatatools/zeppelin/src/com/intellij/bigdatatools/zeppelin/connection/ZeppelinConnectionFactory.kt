package com.intellij.bigdatatools.zeppelin.connection

import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionGroup
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataEx
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataExImpl
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionFactory

class ZeppelinConnectionFactory : RfsConnectionFactory {
  override fun create(
    groupId: BdtConnectionType,
    project: Project?,
    name: String,
    sshConfig: String?,
    url: String,
    additionalData: Map<String, String>,
    sourceConnection: String
  ): ConnectionDataEx? {
    if (groupId != BdtConnectionType.ZEPPELIN)
      return null

    val data = ZeppelinConnectionGroup().createBlankData(perProject = project != null)
    data.uri = url
    data.name = name
    data.anonymous = true
    data.sourceConnection = sourceConnection
    sshConfig?.let {
      data.setTunnelData(ConnectionSshTunnelData(isEnabled = true, configId = it))
    }


    additionalData["sparkVersion"]?.let { data.sparkVersion = it }
    additionalData["scalaVersion"]?.let { data.scalaVersion = it }
    additionalData["hadoopVersion"]?.let { data.hadoopVersion = it }
    additionalData["flinkVersion"]?.let { data.flinkVersion = it }

    return data
  }
}