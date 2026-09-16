package com.jetbrains.bigdatatools.flink.rfs

import com.intellij.bigdatatools.coreUi.connection.BdtSystemProxy
import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.coreUi.settings.connections.CredentialId
import com.intellij.bigdatatools.flink.icons.BigdatatoolsFlinkIcons
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelDataLegacy
import com.jetbrains.bigdatatools.common.connection.tunnel.model.RestClientData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.migrateTunnel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProviderImpl
import com.jetbrains.bigdatatools.flink.settings.FlinkConnectionConfigurable
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle
import javax.swing.Icon

class FlinkConnectionData : RemoteFsDriverProviderImpl(FlinkMessagesBundle.message("config.name.default")), TunnelableData, RestClientData {
  override var operationTimeout: String? = null

  override var headers: Map<String, String>? = null

  var enableBasicAuth = false

  // TODO Unused field left for compatibility. Use proxyEnableType instead.
  @Suppress("unused")
  var enableProxy = false
  var proxyEnableType = ProxyEnableType.DISABLED
  var proxyType = ProxyType.HTTP
  var proxyHost: String = ""
  var proxyPort: Int = 80
  var proxyAuthEnabled = false

  var isFlinkHistory = false

  override fun getIcon(): Icon = BigdatatoolsFlinkIcons.Flink
  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = FlinkDriver(this, project, testConnection = isTest)
  override fun rfsDriverType() = BdtConnectionType.FLINK

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) = FlinkConnectionConfigurable(this, project)

  override var tunnel = ConnectionSshTunnelDataLegacy.DEFAULT

  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  fun getConnectingConfig(): ConnectionConfig {
    val basicAuthCredentials = if (enableBasicAuth) getBasicAuthCredentials() else null
    val proxyCredentials = if (proxyAuthEnabled) getProxyCredentials() else null

    val proxySettings = when (proxyEnableType) {
      ProxyEnableType.GLOBAL -> BdtSystemProxy.getIdeaProxySettings(getUrlWithHttp(uri))
      ProxyEnableType.CUSTOM -> ProxySettings(proxyType, proxyHost.trim(), proxyPort, proxyCredentials)
      ProxyEnableType.DISABLED -> null
    }

    return ConnectionConfig(getUrlWithHttp(uri), groupId, basicAuthCredentials, proxySettings,
                            headers = headers ?: emptyMap())
  }

  private fun getBasicAuthCredentials(): Credentials? = getCredentials(basicCredentialsId)
  private fun getProxyCredentials(): Credentials? = getCredentials(proxyCredentialsId)

  override fun credentialIds() = super.credentialIds() + basicCredentialsId + proxyCredentialsId

  companion object {
    val basicCredentialsId = CredentialId("flink.basic.auth.credentials")
    val proxyCredentialsId = CredentialId("flink.proxy.basic.auth.credentials")
  }
}