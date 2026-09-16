package com.jetbrains.bigdatatools.dataproc.rfs

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionDataImpl
import com.intellij.bigdatatools.dataproc.icons.BigdatatoolsDataprocIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.bigdatatools.common.connection.tunnel.model.RestClientData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.monitoring.data.listener.DataModelListener
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterDriver
import com.jetbrains.bigdatatools.common.rfs.tree.node.RfsDriverTreeNodeBuilder
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionData
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.toolwindow.DataprocToolWindowController
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionData
import javax.swing.Icon

class DataprocDriver(override val connectionData: DataprocConnectionData,
                     project: Project?,
                     testConnection: Boolean) : MonitoringDriver(project, testConnection), MasterDriver {
  override val icon: Icon = BigdatatoolsDataprocIcons.Dataproc

  override val dataManager = DataprocDataManager(project, connectionData, DataprocToolWindowSettings.getInstance())

  override val presentableName: String = "${connectionData.name} [${connectionData.region}]"


  override val treeNodeBuilder: RfsDriverTreeNodeBuilder = object : RfsDriverTreeNodeBuilder() {
    override fun createNode(project: Project, path: RfsPath, driver: Driver) =
      if (path.isCluster)
        DataprocRfsTreeNode(project, path, dataManager.getClusterById(path.name), this@DataprocDriver,
                            isCompound = true)
      else
        DataprocRfsTreeNode(project, path, dataManager.getClusterById(path.name), this@DataprocDriver, isCompound = false)
  }

  init {
    Disposer.register(this, dataManager)

    dataManager.clusterModel.addListener(object : DataModelListener {
      override fun onChanged() = executeOnPooledThread {
        this@DataprocDriver.fileInfoManager.refreshFiles(root)
      }
    })
  }

  override fun getMetaInfoProvider() = DataprocFileMetaInfoProvider(this)

  override fun getController(project: Project) = DataprocToolWindowController.getInstance(project)

  override fun dispose() {}

  override fun doLoadChildren(rfsPath: RfsPath) = when {
    rfsPath.isRoot -> dataManager.clusterModel.entries.map { DataprocFileInfo(this, it) }
    rfsPath.isCluster -> emptyList()
    else -> emptyList()
  }

  override fun prepareRefreshDependedDriver(driver: Driver) {
    val slaveConnectionData = driver.connectionData
    val slaveConnectionId = slaveConnectionData.innerId.removeSuffix(ConnectionDataImpl.TEST_SUFFIX)

    val config = getDependConfig(slaveConnectionId)
                 ?: throw BdtConnectionException(DataprocMessagesBundle.message("error.connection.is.not.found"))

    when {
      slaveConnectionData.groupId == BdtConnectionType.GCS.id -> {}
      slaveConnectionData.groupId == BdtConnectionType.SFTP.id -> {
        val freshConfig = prepareFreshSshTunnel(slaveConnectionId)
        (slaveConnectionData as SftpConnectionData).sshId = freshConfig.id
      }

      config.byComponentGateway -> {
        val token = dataManager.driverCreator.getAccessToken()
        (slaveConnectionData as RestClientData).headers = mapOf("Proxy-Authorization" to "Bearer ${token}")
      }
      !config.byComponentGateway -> {
        val freshConfig = prepareFreshSshTunnel(slaveConnectionId)
        (slaveConnectionData as TunnelableData).setTunnelData(slaveConnectionData.getTunnelData().copy(configId = freshConfig.id))
      }
    }
  }

  private fun prepareFreshSshTunnel(slaveConnectionId: String): SshConfig {
    val config = getDependConfig(slaveConnectionId)
    config ?: throw BdtConnectionException(DataprocMessagesBundle.message("error.connection.is.not.found"))

    dataManager.driverCreator.revalidateSshKeysIfRequired(instanceName = config.instanceName, zone = config.zone)
    return dataManager.driverCreator.updateSshConfig(instanceName = config.instanceName, zone = config.zone,
                                                     projectId = connectionData.projectId ?: "", withModal = false, project = project)
  }

  override fun listDependConnections(rfsPath: RfsPath): List<String> {
    return connectionData.getSlaveConnections().filter { it.clusterId == rfsPath.name }.map { it.connectionId }
  }

  override fun getDependConnectionRfsPath(connectionId: String): RfsPath? {
    val clusterId = getDependConfig(connectionId)?.clusterId ?: return null
    return createRfsPath(clusterId.withSlash())
  }

  private fun getDependConfig(slaveConnectionId: String) = connectionData.getSlaveDriverConfig(slaveConnectionId)
                                                           ?: dataManager.dependsManager.dependedDrivers[slaveConnectionId]

  companion object {
    val RfsPath.isCluster
      get() = size == 1
  }
}