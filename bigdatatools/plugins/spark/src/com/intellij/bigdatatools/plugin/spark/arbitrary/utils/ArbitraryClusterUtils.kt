package com.intellij.bigdatatools.plugin.spark.arbitrary.utils

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionDataImpl
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionData
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionGroup
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.settings.SparkConnectionGroup

object ArbitraryClusterUtils {
  fun createSftp(connectionData: ArbitraryClusterConnectionData): SftpConnectionData {
    val sshId = connectionData.sshId.takeIf { it.isNotBlank() }
    val sftpConnData = SftpConnectionGroup().createBlankData(perProject = connectionData.isPerProject)
    sftpConnData.sshId = sshId ?: ""
    sftpConnData.sourceConnection = connectionData.innerId.removeSuffix(ConnectionDataImpl.TEST_SUFFIX)
    sftpConnData.name += " " + connectionData.name

    return sftpConnData
  }

  fun createSparkConnection(connectionData: ArbitraryClusterConnectionData): SparkConnectionData {
    val sshId = connectionData.sshId.takeIf { it.isNotBlank() }
    val sparkConnData = SparkConnectionGroup().createBlankData(perProject = connectionData.isPerProject)
    if (sshId != null)
      sparkConnData.setTunnelData(ConnectionSshTunnelData(isEnabled = true, configId = sshId, localPort = null))
    sparkConnData.uri = DEFAULT_SPARK_URL
    sparkConnData.sourceConnection = connectionData.innerId.removeSuffix(ConnectionDataImpl.TEST_SUFFIX)
    sparkConnData.name += " " + connectionData.name

    return sparkConnData
  }

  const val DEFAULT_SPARK_URL = "http://localhost:18080"
}