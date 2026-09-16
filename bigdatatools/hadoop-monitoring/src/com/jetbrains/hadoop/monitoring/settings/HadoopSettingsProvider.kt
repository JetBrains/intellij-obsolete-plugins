package com.jetbrains.hadoop.monitoring.settings

import com.intellij.bigdatatools.coreUi.connection.BdtSystemProxy
import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType.CUSTOM
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType.DISABLED
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType.GLOBAL
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.coreUi.settings.connections.CredentialId
import com.intellij.bigdatatools.coreUi.util.BdtUrlUtils
import com.intellij.bigdatatools.hadoopMonitoring.icons.BigdatatoolsHadoopMonitoringIcons
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelDataLegacy
import com.jetbrains.bigdatatools.common.connection.tunnel.model.RestClientData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.migrateTunnel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProviderImpl
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider
import com.jetbrains.hadoop.monitoring.rfs.driver.HadoopMonitoringDriver
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData.Companion.BASIC_CREDENTIALS_ID
import javax.swing.Icon

class HadoopSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.METASTORE_CORE
  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(HadoopConnectionGroup())
}

class HadoopConnectionData : RemoteFsDriverProviderImpl(), TunnelableData, RestClientData {
  override var operationTimeout: String? = null

  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = HadoopMonitoringDriver(project, this, testConnection = isTest)

  override fun getIcon(): Icon = BigdatatoolsHadoopMonitoringIcons.ToolWindowHadoop

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    HadoopConnectionConfigurable(this, project)

  var enableBasicAuth = false

  // TODO Unused field left for compatibility. Use proxyEnableType instead.
  var enableProxy = false
  var proxyEnableType = DISABLED
  var proxyType = ProxyType.HTTP
  var proxyHost: String = ""
  var proxyPort: Int = 80
  var proxyAuthEnabled = false

  var sparkMonitoringDriverId: String = ""

  override var headers: Map<String, String>? = null

  override var tunnel: ConnectionSshTunnelDataLegacy = ConnectionSshTunnelDataLegacy.DEFAULT

  fun getProxyCredentials(): Credentials? = getCredentials(PROXY_CREDENTIALS_ID)

  override fun credentialIds() = super.credentialIds() + BASIC_CREDENTIALS_ID + PROXY_CREDENTIALS_ID

  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  override fun rfsDriverType() = BdtConnectionType.YARN

  companion object {
    val BASIC_CREDENTIALS_ID = CredentialId("hadoop.monitoring.basic.auth.credentials")
    val PROXY_CREDENTIALS_ID = CredentialId("hadoop.monitoring.proxy.basic.auth.credentials")
  }
}

fun HadoopConnectionData.toConnectionConfig(): ConnectionConfig {
  val url = BdtUrlUtils.getFullHttpUrl(uri)

  val proxy = when (proxyEnableType) {
    GLOBAL -> BdtSystemProxy.getIdeaProxySettings(url)
    CUSTOM -> ProxySettings(proxyType, proxyHost.trim(), proxyPort, if (proxyAuthEnabled) getProxyCredentials() else null)
    DISABLED -> null
  }

  return ConnectionConfig(
    url = url,
    connectionGroupId = groupId,
    proxy = proxy,
    basicAuthCredentials = if (enableBasicAuth) getCredentials(BASIC_CREDENTIALS_ID) else null,
    headers = headers ?: emptyMap()
  )
}

