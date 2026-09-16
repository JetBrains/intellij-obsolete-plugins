package com.jetbrains.spark.monitoring.settings

import com.intellij.bigdatatools.coreUi.connection.BdtSystemProxy
import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.coreUi.settings.connections.CredentialId
import com.intellij.bigdatatools.coreUi.settings.connections.httpUrl
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
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
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import javax.swing.Icon

class SparkConnectionData : RemoteFsDriverProviderImpl(), TunnelableData, RestClientData {
  override var operationTimeout: String? = null

  @Transient
  private val historyServerChangedListeners = mutableListOf<() -> Unit>()

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

  override var tunnel = ConnectionSshTunnelDataLegacy.DEFAULT

  override fun credentialIds() = super.credentialIds() + BASIC_CREDENTIALS_ID + PROXY_CREDENTIALS_ID

  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  private fun getBasicAuthCredentials(): Credentials? = getCredentials(BASIC_CREDENTIALS_ID)
  private fun getProxyCredentials(): Credentials? = getCredentials(PROXY_CREDENTIALS_ID)

  var historyServer = true
    set(value) {
      if (field != value) {
        field = value
        historyServerChangedListeners.forEach { it.invoke() }
      }
    }

  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver =
    SparkMonitoringDriver(this, project, testConnection = isTest)

  override fun getIcon(): Icon = BigdatatoolsSparkMonitoringIcons.Spark

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    SparkConnectionConfigurable(this, project)

  fun getConnectionConfig(): ConnectionConfig {
    val basicAuthCredentials = if (enableBasicAuth) getBasicAuthCredentials() else null
    val proxyCredentials = if (proxyAuthEnabled) getProxyCredentials() else null

    val proxySettings = when (proxyEnableType) {
      ProxyEnableType.GLOBAL -> BdtSystemProxy.getIdeaProxySettings(httpUrl())
      ProxyEnableType.CUSTOM -> ProxySettings(proxyType, proxyHost.trim(), proxyPort, proxyCredentials)
      ProxyEnableType.DISABLED -> null
    }

    return ConnectionConfig(getUrlWithHttp(uri), groupId, basicAuthCredentials, proxySettings,
                            headers = headers ?: emptyMap())
  }

  override fun rfsDriverType() = BdtConnectionType.SPARK_MONITORING

  companion object {
    val BASIC_CREDENTIALS_ID = CredentialId("spark.monitoring.basic.auth.credentials")
    val PROXY_CREDENTIALS_ID = CredentialId("spark.monitoring.proxy.basic.auth.credentials")
  }
}