package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.dataproc.icons.BigdatatoolsDataprocIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProvider
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.dataproc.rfs.DataprocDriver
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.dataproc.util.GcRegion
import com.jetbrains.bigdatatools.gcloud.connection.GCloudData
import javax.swing.Icon

@Suppress("MemberVisibilityCanBePrivate")
class DataprocConnectionData : GCloudData(
  DataprocMessagesBundle.message("group.name.dataproc")), MasterConnectionData<DataprocSlaveConnection>, RemoteFsDriverProvider {
  var region: String = GcRegion.us_west2.id

  var sshConfigs: MutableMap<String, String> = mutableMapOf()
  override fun sshConfigsByClusterId(): Map<String, String> = sshConfigs

  var slaveDriverJsons: MutableList<String> = mutableListOf()

  override fun getIcon(): Icon = BigdatatoolsDataprocIcons.Dataproc

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    object : ConnectionConfigurable<DataprocConnectionData, DataprocSettingsCustomizer>(this, project, parentGroup.icon) {
      override fun getHelpTopic() = "big.data.tools.gc.dataproc"
      override fun createSettingsCustomizer() = DataprocSettingsCustomizer(project, connectionData, disposable, coroutineScope)
      override fun createConnectionTesting() = DataprocConnectionTesting(project, settingsCustomizer)
    }

  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = DataprocDriver(this, project, testConnection = isTest)

  override fun rfsDriverType() = BdtConnectionType.DATAPROC

  override fun getSlaveConnections(): List<DataprocSlaveConnection> = slaveDriverJsons.map {
    BdtJson.fromJsonToClass(it, DataprocSlaveConnection::class.java)
  }

  override fun getSlaveDriverConfig(connectionId: String): DataprocSlaveConnection? = synchronized(this) {
    getSlaveConnections().firstOrNull { it.connectionId == connectionId }
  }

  override fun setSlaveDriverConfig(conn: DataprocSlaveConnection) = synchronized(this) {
    slaveDriverJsons += BdtJson.toJson(conn)
  }

  override fun removeSlave(innerIds: Set<String>): Boolean = synchronized(this) {
    val slaveDriversZip = getSlaveConnections().zip(slaveDriverJsons)
    val forRemove = slaveDriversZip.filter { it.first.connectionId in innerIds }.map { it.second }
    slaveDriverJsons.removeAll(forRemove)
  }
}