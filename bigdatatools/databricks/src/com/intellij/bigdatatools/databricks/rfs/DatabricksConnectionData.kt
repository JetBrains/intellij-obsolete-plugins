package com.intellij.bigdatatools.databricks.rfs

import com.intellij.bigdatatools.databricks.client.DatabricksConnType
import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.bigdatatools.databricks.settings.DatabricksConnectionConfigurable
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.bigdatatools.databricks.util.DatabricksProfiles
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.coreUi.settings.connections.CredentialId
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelDataLegacy
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.migrateTunnel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProviderImpl
import javax.swing.Icon

class DatabricksConnectionData : RemoteFsDriverProviderImpl(DatabricksBundle.message("config.name.default")), TunnelableData {
  override fun getIcon(): Icon = BigdatatoolsDatabricksIcons.Databricks
  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = DatabricksDriver(this, project, testConnection = isTest)
  override fun rfsDriverType() = BdtConnectionType.DATABRICKS

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) = DatabricksConnectionConfigurable(this, project)

  var connType: DatabricksConnType = DatabricksConnType.PROFILE
  var profile: String? = null
  var connectedClusterId: String = ""
  var customRemotePath: String? = null

  override var tunnel = ConnectionSshTunnelDataLegacy.DEFAULT

  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  fun getRealUri(): String {
    return if (connType == DatabricksConnType.PROFILE) {
      // We are reading URL from profile, it was hidden in settings.
      val profile = this.profile
      if (profile == null)
        ""
      else
        DatabricksProfiles.getProfileHost(profile) ?: ""
    }
    else {
      uri
    }
  }

  override fun credentialIds() = super.credentialIds() + TOKEN_ID_KEY

  companion object {
    @Suppress("SpellCheckingInspection")
    val TOKEN_ID_KEY = CredentialId("Databrics.TokenId")
  }
}