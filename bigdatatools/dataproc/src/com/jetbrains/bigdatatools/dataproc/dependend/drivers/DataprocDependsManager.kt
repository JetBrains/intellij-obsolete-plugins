package com.jetbrains.bigdatatools.dataproc.dependend.drivers

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.use
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshUiData
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.copypaste.RfsCopyPasteManager
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.depend.AbstractDriverDependsManager
import com.jetbrains.bigdatatools.common.rfs.driver.refreshConnectionBlocking
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionDataEx
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionFactory
import com.jetbrains.bigdatatools.common.util.BdtSshUtils
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManagerUtils
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo
import com.jetbrains.bigdatatools.dataproc.settings.DataprocSlaveConnection
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.gcloud.auth.GcloudAuthConst
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DataprocDependsManager(override val dataManager: DataprocDataManager) :
  AbstractDriverDependsManager<DataprocClusterInfo, DataprocWebInterfaceInfo, DataprocSlaveConnection>() {

  val dependedDrivers = mutableMapOf<String, DataprocSlaveConnection>()

  override val connectionData = dataManager.connectionData

  init {
    removeObsoleteConnections()
  }

  override fun dispose() {}

  fun browseGcsStageBucket(project: Project, cluster: DataprocClusterInfo, path: String?) {
    val configBucket = cluster.stagingBucket
    val info = DataprocWebInterfaceInfo(DataprocWebInterfaceInfo.STAGE_BUCKET_NAME,
                                        configBucket,
                                        componentGateway = true,
                                        instanceName = "${cluster.name}-m")
    browseOrOpenConnection(project, cluster, info, path?.split("://")?.last())
  }


  override fun findStored(project: Project, clusterId: String, appInfo: DataprocWebInterfaceInfo): ConnectionData? {
    val slaveDrivers = connectionData.getSlaveConnections()
    val slaveConnection = slaveDrivers
                            .firstOrNull {
                              it.connectionType == appInfo.connType &&
                              it.clusterId == clusterId &&
                              (appInfo.instanceName.isBlank() || appInfo.instanceName == it.instanceName)
                            }
                          ?: return null
    return RfsConnectionDataManager.instance?.getConnectionById(project, slaveConnection.connectionId)
  }

  override fun removeDependInfo(dependConnection: ConnectionData) {
    connectionData.removeSlave(setOf(dependConnection.innerId))
  }

  override fun openInBrowser(project: Project, cluster: DataprocClusterInfo, appInfo: DataprocWebInterfaceInfo) {
    if (!DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, connectionData)) {
      return
    }

    if (appInfo.componentGateway)
      invokeLater {
        BrowserUtil.browse(appInfo.url)
      }
    else {
      val sshConfig = dataManager.driverCreator.updateSshConfig(project, instanceName = cluster.name + "-m", zone = cluster.zone,
                                                                withModal = true, projectId = connectionData.projectId!!)

      val uri = appInfo.url
      val tunnelHandler = BdtSshTunnelService.createIfRequired(project, ConnectionSshTunnelData(isEnabled = true, sshConfig.id), uri, uri,
                                                               isTest = false) ?: return
      Disposer.register(dataManager, tunnelHandler)

      val tunnelledUri = tunnelHandler.tunnelledUri
      invokeLater {
        BrowserUtil.browse(tunnelledUri)
      }
    }
  }

  override fun createConnection(project: Project, cluster: DataprocClusterInfo, appInfo: DataprocWebInterfaceInfo): ConnectionDataEx? {
    if (appInfo.connType != BdtConnectionType.GCS && !DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, connectionData)) {
      return null
    }

    val realAppInfo = when (appInfo.connType) {
      null -> return null
      BdtConnectionType.ZEPPELIN -> appInfo.copy(url = "localhost:8080", componentGateway = false)
      else -> appInfo
    }

    return when {
      realAppInfo.componentGateway -> createPlainAppConnection(cluster, realAppInfo)
      else -> createSshAppConnection(project, cluster, realAppInfo)
    }
  }

  override fun storeDependDriverInfo(project: Project,
                                     cluster: DataprocClusterInfo,
                                     appInfo: DataprocWebInterfaceInfo,
                                     connection: ConnectionData) {
    connectionData.setSlaveDriverConfig(DataprocSlaveConnection.createFor(cluster, appInfo, connection))
  }


  private fun createSshAppConnection(project: Project, cluster: DataprocClusterInfo, appInfo: DataprocWebInterfaceInfo,
                                     withModal: Boolean = false): ConnectionDataEx? {
    val connType = appInfo.connType ?: return null


    val sshConfig = getOrCreateSshConfig(project, cluster, withModal) ?: return null

    return RfsConnectionFactory.create(
      groupId = connType,
      project = dataManager.project,
      name = "${connType.connName} ${cluster.name}",
      sshConfig = sshConfig.id,
      url = appInfo.url,
      sourceConnection = connectionData.innerId,
      additionalData = emptyMap())
  }

  @Suppress("UNCHECKED_CAST")
  private fun createPlainAppConnection(cluster: DataprocClusterInfo, appInfo: DataprocWebInterfaceInfo): ConnectionDataEx? {
    val connType = appInfo.connType ?: return null
    val additionalData = mapOf(
      GcloudAuthConst.PROJECT_ID to connectionData.projectId,
      GcloudAuthConst.AUTH_TYPE to connectionData.authType,
      GcloudAuthConst.JSON_LOCATION to connectionData.jsonLocation,
    ).filterValues { it != null } as Map<String, String>


    return RfsConnectionFactory.create(
      groupId = appInfo.connType,
      project = dataManager.project,
      name = "${connType.connName} ${cluster.name}",
      sshConfig = null,
      url = appInfo.url,
      sourceConnection = connectionData.innerId,
      additionalData = additionalData)
  }

  fun downloadFileToTemp(project: Project, indicator: ProgressIndicator, artifactPath: FilePath, clusterName: String): File? {
    val cluster = dataManager.getClusterByName(clusterName) ?: return null

    val instanceName = "$clusterName-m"
    val webInfo = when (artifactPath.type) {
      FileType.FILE -> DataprocWebInterfaceInfo(name = instanceName, url = "", componentGateway = false,
                                                instanceName = instanceName)
      FileType.GCS -> DataprocWebInterfaceInfo(DataprocWebInterfaceInfo.GCS_NAME, url = "", componentGateway = true,
                                               instanceName = instanceName)
      else -> throw Exception(DataprocMessagesBundle.message("resolve.artifact.is.not.supported", artifactPath.type))
    }

    val connectionData = createConnection(project, cluster, webInfo) ?: return null

    val slaveConnection = DataprocSlaveConnection.createFor(cluster, webInfo, connectionData)
    val rfsDriver = connectionData.createDriver(project, isTest = true)

    val file = FileUtil.createTempFile("bdt-dataproc", "temp-jar.jar", true)
    rfsDriver.use {
      dependedDrivers[connectionData.innerId] = slaveConnection
      rfsDriver.refreshConnectionBlocking(ActivitySource.DATAPROC_DEPENDENT)
      val path = rfsDriver.createRfsPath(artifactPath.path)
      RfsCopyPasteManager.downloadFromRemoteToIoFile(project, indicator, it, path, file)
    }
    dependedDrivers.remove(connectionData.innerId)
    return file
  }

  fun getOrCreateSshConfig(project: Project?, clusterInfo: DataprocClusterInfo, withModal: Boolean): SshConfig? {
    if (!DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, connectionData)) {
      return null
    }


    val instanceName = clusterInfo.name + "-m"
    val isRevalidated = dataManager.driverCreator.revalidateSshKeysIfRequired(instanceName = instanceName,
                                                                              zone = clusterInfo.zone)
    if (!isRevalidated) {
      val cached = getCachedConfig(project, clusterInfo)
      cached?.let { return it }

    }
    return dataManager.driverCreator.updateSshConfig(instanceName = instanceName, zone = clusterInfo.zone,
                                                     projectId = connectionData.projectId ?: "", withModal = withModal, project = project)
  }


  private fun getCachedConfig(project: Project?, clusterInfo: DataprocClusterInfo): SshConfig? {
    val cluster = clusterInfo.cluster
    val configId = connectionData.sshConfigs[cluster.clusterName]
    val storedSshConfigs = SshConfigManager.getInstance(project).configs

    return storedSshConfigs.firstOrNull { configId != null && it.id == configId }
  }


  fun attachConfig(clusterName: String, sshConfig: SshConfig) {
    connectionData.sshConfigs[clusterName] = sshConfig.id
  }

  suspend fun checkAppConnectAvailable(project: Project, cluster: DataprocClusterInfo,
                                       appInfo: DataprocWebInterfaceInfo): Pair<Boolean, String> {
    if (appInfo.connType != BdtConnectionType.GCS && !DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, connectionData)) {
      return false to ""
    }

    val realAppInfo = when (appInfo.connType) {
      null -> return false to ""
      BdtConnectionType.ZEPPELIN -> appInfo.copy(url = "localhost:8080", componentGateway = false)
      else -> appInfo
    }

    if (realAppInfo.componentGateway)
      return true to ""
    val sshConfig = withContext(Dispatchers.IO) {
      getOrCreateSshConfig(project, cluster, false)
    } ?: return false to ""
    return BdtSshUtils.testConnectionAndWrapResult(SshUiData.create(sshConfig), project)
  }

  companion object {
    fun getSparkApp(cluster: DataprocClusterInfo) = DataprocDataManager.getApplications(cluster).firstOrNull {
      it.connType == BdtConnectionType.SPARK_MONITORING
    }
  }
}