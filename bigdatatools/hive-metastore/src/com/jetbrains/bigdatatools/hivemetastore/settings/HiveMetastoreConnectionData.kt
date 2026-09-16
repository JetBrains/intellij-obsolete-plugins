package com.jetbrains.bigdatatools.hivemetastore.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.hiveMetastore.icons.BigdatatoolsHiveMetastoreIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelDataLegacy
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.migrateTunnel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProviderImpl
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveMetastoreDriver
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import javax.swing.Icon

class HiveMetastoreConnectionData : RemoteFsDriverProviderImpl(HiveMessagesBundle.message("config.name.default")), TunnelableData {
  var tablePattern = "*"
  var databasePattern = "*"

  var propertySource: HiveMetastorePropertySource = HiveMetastorePropertySource.DIRECT
  var configFolderPath: String? = null
  var properties = ""

  var useKerberosTicketCache: Boolean = true

  override var tunnel = ConnectionSshTunnelDataLegacy.DEFAULT

  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    object : ConnectionConfigurable<HiveMetastoreConnectionData, HiveMetastoreSettingsCustomizer>(this, project, parentGroup.icon) {
      override fun getHelpTopic() = "big.data.tools.hive"
      override fun createSettingsCustomizer() = HiveMetastoreSettingsCustomizer(project, connectionData, disposable)
    }

  override fun getIcon(): Icon = BigdatatoolsHiveMetastoreIcons.Apache_hive
  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = HiveMetastoreDriver(this, project, isTest)
  override fun rfsDriverType() = BdtConnectionType.HIVE
}