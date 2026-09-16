package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProviderImpl
import com.jetbrains.bigdatatools.common.settings.DoNotSerialize
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.sftp.settings.SshableConnectionData
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import javax.swing.Icon

class ArbitraryClusterConnectionData : RemoteFsDriverProviderImpl(), SshableConnectionData, MasterConnectionData<ArbitraryClusterSlaveConnection> {
  @DoNotSerialize
  var sparkMonitoringDriverId: String
    get() = getSlaveConnection(BdtConnectionType.SPARK_MONITORING) ?: ""
    set(value) = updateSlaveConnection(value, BdtConnectionType.SPARK_MONITORING)

  @DoNotSerialize
  var sftpDriverId: String
    get() = getSlaveConnection(BdtConnectionType.SFTP) ?: ""
    set(value) = updateSlaveConnection(value, BdtConnectionType.SFTP)

  override var sshId: String = ""

  override fun sshConfigsByClusterId(): Map<String, String> = mapOf(SINGLE_CLUSTER_ID to sshId)

  @Suppress("MemberVisibilityCanBePrivate")
  var slaveDriverJsons: MutableList<String> = mutableListOf()

  override fun getIcon(): Icon = BigdatatoolsPluginSparkIcons.ArbitraryCluster

  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = ArbitraryClusterDriver(this, project, isTest)

  override fun rfsDriverType(): BdtConnectionType = BdtConnectionType.ARBITRARY_CLUSTER

  override fun createConfigurable(project: Project,
                                  parentGroup: ConnectionGroup) = ArbitraryClusterConnectionConfigurable(this, project)

  override fun getSlaveConnections() = slaveDriverJsons.map {
    BdtJson.fromJsonToClass(it, ArbitraryClusterSlaveConnection::class.java)
  }

  override fun getSlaveDriverConfig(connectionId: String) = synchronized(this) {
    getSlaveConnections().firstOrNull { it.connectionId == connectionId }
  }

  override fun setSlaveDriverConfig(conn: ArbitraryClusterSlaveConnection) = synchronized(this) {
    slaveDriverJsons += BdtJson.toJson(conn)
  }

  override fun removeSlave(innerIds: Set<String>) = synchronized(this) {
    val slaveDriversZip = getSlaveConnections().zip(slaveDriverJsons)
    val forRemove = slaveDriversZip.filter { it.first.connectionId in innerIds }.map { it.second }
    slaveDriverJsons.removeAll(forRemove)
  }

  fun getSparkConnection(project: Project?): SparkConnectionData? =
    RfsConnectionDataManager.instance?.getConnectionById(project = project, sparkMonitoringDriverId) as? SparkConnectionData

  private fun getSlaveConnection(connectionType: BdtConnectionType) =
    getSlaveConnections().firstOrNull { it.connectionType == connectionType }?.connectionId

  private fun updateSlaveConnection(value: String, connectionType: BdtConnectionType) {
    val forRemove = getSlaveConnection(connectionType)
    removeSlave(setOfNotNull(forRemove))
    if (value.isBlank())
      return
    setSlaveDriverConfig(ArbitraryClusterSlaveConnection(value, connectionType))
  }

  companion object {
    val SINGLE_CLUSTER_ID = "ARBITRARY_CLUSTER"
  }
}