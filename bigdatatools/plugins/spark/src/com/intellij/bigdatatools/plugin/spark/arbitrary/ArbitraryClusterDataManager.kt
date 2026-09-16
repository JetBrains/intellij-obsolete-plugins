package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.arbitrary.datamanager.ArbitraryClusterInfo
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.MonitoringDataManager
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.spark.submit.run.cluster.ui.AttachedSshConfigsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class ArbitraryClusterDataManager(project: Project?,
                                  override val connectionData: ArbitraryClusterConnectionData,
                                  override val settings: ArbitraryClusterToolWindowSettings) : MonitoringDataManager(project, settings) {
  override val client = ArbitraryClusterClient(project, connectionData)

  val dependsManager = ArbitraryClusterDependsManager(this).also { Disposer.register(this, it) }

  init {
    init()
  }

  override fun onSuccessfulConnect() {
    val dependDrivers = connectionData.getSlaveConnections().mapNotNull { DriverManager.getDriverById(project, it.connectionId) }
    SafeExecutor.instance.asyncSuspend("Refresh Depend", timeout = Duration.INFINITE) {
      delay(500)
      dependDrivers.forEach {
        it.refreshConnection(ActivitySource.ARBITRARY_CLUSTER_DEPENDENT)
      }
    }
  }

  suspend fun chooseFromUiConnectionData(project: Project?): SshConfig? {
    val originSshConfig = getSshConfig(project) ?: return null
    val currentUiData = SshUiData.create(originSshConfig)
    return withContext(Dispatchers.EDT) {
      val chosenConfig = AttachedSshConfigsUtil.editAttachedSshConfigSetting(project, currentUiData, connectionData.name) {
        connectionData.sshId = it.id
      } ?: return@withContext null
      return@withContext chosenConfig
    }
  }

  fun getSshConfig(project: Project?) = SshConfigManager.getInstance(project).findConfigById(connectionData.sshId)

  fun createSparkConnection(project: Project, onInit: (ConnectionData) -> Unit = {}): ConnectionData? {
    val appInfo = ArbitraryClusterAppInfo(BdtConnectionType.SPARK_MONITORING)
    val connectionData = dependsManager.getOrCreateDefault(project, ArbitraryClusterInfo(), appInfo) ?: return null
    onInit(connectionData)
    return connectionData
  }


  companion object {
    fun getInstance(connectionId: String, project: Project): ArbitraryClusterDataManager? =
      (DriverManager.getDriverById(project, connectionId) as? ArbitraryClusterDriver)?.dataManager
  }
}