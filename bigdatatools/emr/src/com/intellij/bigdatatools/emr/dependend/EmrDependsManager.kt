package com.intellij.bigdatatools.emr.dependend

import com.intellij.bigdatatools.awsBase.settings.AwsCompatibleConnectionDataBase
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterAppInfo
import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.model.EmrClusterInfo
import com.intellij.bigdatatools.emr.model.EmrSlaveConnection
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.depend.AbstractDriverDependsManager
import com.jetbrains.bigdatatools.common.rfs.driver.depend.OpenInDependentDriverPopup
import com.jetbrains.bigdatatools.common.rfs.driver.refreshConnectionLaunch
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionFactory
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.sftp.rfs.driver.SftpDriver
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionGroup
import org.com.jetbrains.bigdatatools.S3ConnectionGroup
import org.com.jetbrains.bigdatatools.aws.s3.S3ConnectionData

class EmrDependsManager(override val dataManager: EmrDataManager) :
  AbstractDriverDependsManager<EmrClusterDetails, EmrClusterAppInfo, EmrSlaveConnection>() {

  override val connectionData = dataManager.connectionData

  private val sshManager
    get() = dataManager.sshManager

  init {
    removeObsoleteConnections()
  }

  override fun dispose() {}


  suspend fun createSftpDriver(project: Project, dataManager: EmrDataManager, clusterId: String): SftpDriver? {
    val details = dataManager.getClusterDetails(clusterId)

    val sshConfig = sshManager.getOrAskSetupOrShowError(project, details) ?: return null

    val sftpConnectionData = SftpConnectionGroup().createBlankData().also {
      it.name = details.cluster.name() + "-master"
    }

    val driver = SftpDriver(project, sftpConnectionData, sshConfig)
    driver.refreshConnectionLaunch(ActivitySource.EMR_DEPENDENT)
    return driver
  }


  override fun findStored(project: Project, clusterId: String, appInfo: EmrClusterAppInfo): ConnectionData? {
    val slaveDrivers = connectionData.getSlaveConnections()
    return slaveDrivers.filter { slaveConnection ->
      slaveConnection.connectionType == appInfo.connType && appInfo.url == slaveConnection.url
    }.firstNotNullOfOrNull { slaveConnection ->
      RfsConnectionDataManager.instance?.getConnectionById(project, slaveConnection.connectionId)
    }
  }

  override fun removeDependInfo(dependConnection: ConnectionData) {
    connectionData.removeSlave(setOf(dependConnection.innerId))
  }

  override fun openInBrowser(project: Project, cluster: EmrClusterDetails, appInfo: EmrClusterAppInfo) {
    val appUri = appInfo.url.ifBlank { return }
    val uri = if (!cluster.isInternal) {
      val sshConfig = runBlockingMaybeCancellable {
        sshManager.getOrAskSetupOrShowError(project, cluster)
      } ?: return

      val tunnelData = ConnectionSshTunnelData(isEnabled = true, sshConfig.id)
      val tunnelHandler = BdtSshTunnelService.createIfRequired(project, tunnelData,
                                                               uri = appUri,
                                                               connectionId = connectionData.innerId,
                                                               isTest = false) ?: return
      Disposer.register(dataManager, tunnelHandler)
      tunnelHandler.tunnelledUri
    }
    else {
      appUri
    }

    invokeLater {
      BrowserUtil.browse(uri)
    }
  }


  fun createConnection(project: Project, cluster: EmrClusterInfo, connectionType: BdtConnectionType): Unit = dataManager.actionWrapper {
    val clusterDetails = dataManager.loadClusterById(cluster.id)
    val apps = clusterDetails.getApps()
    val foundApp: EmrClusterAppInfo = apps.firstOrNull { it.connType == connectionType } ?: return@actionWrapper
    browseOrOpenConnection(project, clusterDetails, foundApp, OpenInDependentDriverPopup.DialogPopup) {
      dataManager.driver.fileInfoManager.refreshFiles(RfsPath(listOf(cluster.id), true))
    }
  }

  override fun createConnection(project: Project, cluster: EmrClusterDetails, appInfo: EmrClusterAppInfo): ConnectionData? {
    val connType = appInfo.connType
    if (connType == BdtConnectionType.S3)
      return createS3ConnectionData(appInfo.url)

    if (connType == null) {
      return null
    }
    val sshConfig = if (!cluster.isInternal) {
      runBlockingMaybeCancellable {
        sshManager.getOrAskSetupOrShowError(project, cluster) ?: return@runBlockingMaybeCancellable null
      }
    }
    else
      null


    return RfsConnectionFactory.create(
      groupId = connType,
      project = dataManager.project,
      name = "${connType.connName} ${cluster.cluster.name()}",
      sshConfig = sshConfig?.id,
      url = appInfo.url,
      sourceConnection = connectionData.innerId,
      additionalData = emptyMap())
  }

  override fun storeDependDriverInfo(project: Project,
                                     cluster: EmrClusterDetails,
                                     appInfo: EmrClusterAppInfo,
                                     connection: ConnectionData) {
    connectionData.setSlaveDriverConfig(EmrSlaveConnection.createFor(cluster, appInfo, connection))
  }


  fun createS3ConnectionData(path: String): S3ConnectionData {
    val emrConnectionData = dataManager.connectionData

    val s3ConnData = S3ConnectionGroup().createBlankData()
    s3ConnData.name = "Logs"

    s3ConnData.sourceConnection = emrConnectionData.innerId

    s3ConnData.proxyEnableType = emrConnectionData.proxyEnableType
    s3ConnData.proxyHost = emrConnectionData.proxyHost
    s3ConnData.proxyPort = emrConnectionData.proxyPort
    s3ConnData.proxyAuthEnabled = emrConnectionData.proxyAuthEnabled
    s3ConnData.proxyWorkstation = emrConnectionData.proxyWorkstation
    s3ConnData.nonProxyHosts = emrConnectionData.nonProxyHosts
    s3ConnData.isDisableSocketProxy = emrConnectionData.isDisableSocketProxy
    s3ConnData.proxyDomain = emrConnectionData.proxyDomain
    s3ConnData.isPreemptiveBasicProxyAuth = emrConnectionData.isPreemptiveBasicProxyAuth
    s3ConnData.setCredentials(emrConnectionData.getCredentials(AwsCompatibleConnectionDataBase.PROXY_CREDENTIALS_ID),
                              AwsCompatibleConnectionDataBase.PROXY_CREDENTIALS_ID)

    s3ConnData.activeAuthenticationType = emrConnectionData.activeAuthenticationType
    s3ConnData.profileName = emrConnectionData.profileName
    s3ConnData.profileCredentialsPath = emrConnectionData.profileCredentialsPath
    s3ConnData.setCredentials(emrConnectionData.getCredentials(AwsCompatibleConnectionDataBase.SECRET_KEY_ID),
                              AwsCompatibleConnectionDataBase.SECRET_KEY_ID)

    s3ConnData.region = emrConnectionData.region

    s3ConnData.isBucketSourceCustom = true
    s3ConnData.bucket = path
    return s3ConnData
  }
}