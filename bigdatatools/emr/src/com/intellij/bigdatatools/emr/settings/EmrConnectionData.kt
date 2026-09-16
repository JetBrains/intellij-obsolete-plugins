package com.intellij.bigdatatools.emr.settings

import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.aws.driver.AwsCompatibleConnectionData
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.emr.model.EmrSlaveConnection
import com.intellij.bigdatatools.emr.rfs.EmrDriver
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTesting
import org.com.jetbrains.bigdatatools.icons.Icons.EMR_ICON
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import org.jetbrains.annotations.Nls
import software.amazon.awssdk.regions.Region
import javax.swing.Icon

class EmrConnectionData(@Suppress("HardCodedStringLiteral")
                        @Nls
                        override var region: String = Region.US_WEST_2.id(),
                        activeAuthenticationType: String = AuthenticationType.DEFAULT.id,
                        var useCustomEndpoint: Boolean = false,
                        var customEndpoint: String = "",
                        var customRegion: String = "") : AwsCompatibleConnectionData(activeAuthenticationType,
                                                                                     connectionName = HdfsMessagesBundle.message(
                                                                                       "group.name.emr")),
                                                         MasterConnectionData<EmrSlaveConnection> {
  var sshConfigs: MutableMap<String, String> = mutableMapOf()
  override fun sshConfigsByClusterId(): Map<String, String> = sshConfigs

  @Suppress("MemberVisibilityCanBePrivate")
  var slaveDriverJsons: MutableList<String> = mutableListOf()

  override fun getIcon(): Icon = EMR_ICON

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    object : ConnectionConfigurable<EmrConnectionData, EmrSettingsCustomizer>(this, project, parentGroup.icon) {
      override fun getHelpTopic() = "big.data.tools.amazon.emr"
      override fun createSettingsCustomizer() = EmrSettingsCustomizer(project, connectionData, disposable, coroutineScope)
      override fun createConnectionTesting(): ConnectionTesting<EmrConnectionData> = EmrTesting(project, settingsCustomizer)
    }

  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = EmrDriver(this, project, testConnection = isTest)

  override fun rfsDriverType() = BdtConnectionType.EMR

  override fun getSlaveConnections() = slaveDriverJsons.map {
    BdtJson.fromJsonToClass(it, EmrSlaveConnection::class.java)
  }

  override fun getSlaveDriverConfig(connectionId: String) = synchronized(this) {
    getSlaveConnections().firstOrNull { it.connectionId == connectionId }
  }

  override fun setSlaveDriverConfig(conn: EmrSlaveConnection) = synchronized(this) {
    slaveDriverJsons += BdtJson.toJson(conn)
  }

  override fun removeSlave(innerIds: Set<String>) = synchronized(this) {
    val slaveDriversZip = getSlaveConnections().zip(slaveDriverJsons)
    val forRemove = slaveDriversZip.filter { it.first.connectionId in innerIds }.map { it.second }
    slaveDriverJsons.removeAll(forRemove)
  }
}