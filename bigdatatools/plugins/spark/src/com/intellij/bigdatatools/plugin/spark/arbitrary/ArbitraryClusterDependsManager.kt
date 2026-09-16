package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.arbitrary.datamanager.ArbitraryClusterInfo
import com.intellij.bigdatatools.plugin.spark.arbitrary.utils.ArbitraryClusterUtils
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.depend.AbstractDriverDependsManager
import com.jetbrains.bigdatatools.common.rfs.driver.depend.OpenInDependentDriverPopup
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.monitoring.settings.SparkConnectionData

class ArbitraryClusterDependsManager(override val dataManager: ArbitraryClusterDataManager) :
  AbstractDriverDependsManager<ArbitraryClusterInfo, ArbitraryClusterAppInfo, ArbitraryClusterSlaveConnection>() {

  override val connectionData = dataManager.connectionData

  override fun dispose() {}


  override fun findStored(project: Project, clusterId: String, appInfo: ArbitraryClusterAppInfo): ConnectionData? {
    val slaveDrivers = connectionData.getSlaveConnections()
    return slaveDrivers.filter { slaveConnection ->
      slaveConnection.connectionType == appInfo.connType
    }.firstNotNullOfOrNull { slaveConnection ->
      RfsConnectionDataManager.instance?.getConnectionById(project, slaveConnection.connectionId)
    }

  }

  override fun openInBrowser(project: Project, cluster: ArbitraryClusterInfo, appInfo: ArbitraryClusterAppInfo) {
    if (appInfo.connType != BdtConnectionType.SPARK_MONITORING)
      return
    val data = findStored(project, "", appInfo) as? SparkConnectionData
               ?: ArbitraryClusterUtils.createSparkConnection(connectionData)

    invokeLater {
      BrowserUtil.browse(data.uri)
    }
  }

  fun createConnection(project: Project, connectionType: BdtConnectionType) {
    browseOrOpenConnection(project, ArbitraryClusterInfo(), ArbitraryClusterAppInfo(connectionType),
                           OpenInDependentDriverPopup.DialogPopup) {
      dataManager.driver.fileInfoManager.refreshFiles(RfsPath(listOf(connectionType.connName), true))
    }
  }


  override fun createConnection(project: Project, cluster: ArbitraryClusterInfo, appInfo: ArbitraryClusterAppInfo): ConnectionData {
    val newConnection = when (appInfo.connType) {
      BdtConnectionType.SPARK_MONITORING -> ArbitraryClusterUtils.createSparkConnection(connectionData)
      BdtConnectionType.SFTP -> ArbitraryClusterUtils.createSftp(connectionData)
      else -> error("Not supported connection type: ${appInfo.connType}")
    }
    return newConnection
  }

  override fun storeDependDriverInfo(project: Project,
                                     cluster: ArbitraryClusterInfo,
                                     appInfo: ArbitraryClusterAppInfo,
                                     connection: ConnectionData) {
    val connectionType = appInfo.connType ?: return
    connectionData.setSlaveDriverConfig(ArbitraryClusterSlaveConnection(connection.innerId, connectionType))
  }

  override fun removeDependInfo(dependConnection: ConnectionData) {
    connectionData.removeSlave(setOf(dependConnection.innerId))
  }
}