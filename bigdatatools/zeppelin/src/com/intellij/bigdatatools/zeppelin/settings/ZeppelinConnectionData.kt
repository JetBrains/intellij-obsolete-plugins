@file:Suppress("unused")

package com.intellij.bigdatatools.zeppelin.settings

import com.intellij.bigdatatools.coreUi.connection.BdtSystemProxy
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.connection.oauth.ConnectionCookieStore
import com.intellij.bigdatatools.coreUi.connection.oauth.CookieAvailable
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.coreUi.settings.connections.CredentialId
import com.intellij.bigdatatools.coreUi.util.BdtUrlUtils
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.ztools.offers.ZtoolsOfferService
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsConfig
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsSqlCollectStrategy
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsSqlSettings
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsVariablesSettings
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelDataLegacy
import com.jetbrains.bigdatatools.common.connection.tunnel.model.RestClientData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.migrateTunnel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.settings.RemoteFsDriverProviderImpl
import com.jetbrains.bigdatatools.common.settings.DoNotSerialize
import org.apache.http.cookie.CookieOrigin
import org.apache.http.impl.cookie.BasicClientCookie
import org.jetbrains.annotations.Contract
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import javax.swing.Icon
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate")
class ZeppelinConnectionData : RemoteFsDriverProviderImpl(), TunnelableData, CookieAvailable, RestClientData {
  override var operationTimeout: String? = null
  override var headers: Map<String, String>? = null

  override fun getIcon(): Icon = ZeppelinDriver.driverIcon

  // Never make any "val" field of this class private, because of serialization issues.
  var defaultNotebookName: String = "IDEA_Plugin/default"

  var systemNotificationEnabled: Boolean = true
  var notifyAfter: Int = 60

  var sparkVersion: String = "2.2.0"
  var scalaVersion: String = "2.11"
  var hadoopVersion: String = "2.7.3"
  var flinkVersion: String = "1.14.5"
  var zeppelinVersion: String = ""

  var enableBasicAuth = false

  //Ztools
  @Deprecated(message = "Migrated to new config", replaceWith = ReplaceWith("isZtoolsEnabled"))
  var enableIntellijIntegration: Boolean? = null

  @Deprecated(message = "Use ztoolsConfJson")
  var enableOnDemandZtools: Boolean? = null

  @Deprecated(message = "Use ztoolsConfJson")
  var ztoolsDfsSameNoteOnly: Boolean? = null

  @Deprecated(message = "Use ztoolsConfJson")
  var ztoolsSqlSameNoteOnly: Boolean? = null

  @Deprecated(message = "Use ztoolsConfJson")
  var ztoolsCalculateTablesOnDemandOnly: Boolean? = null

  var isZtoolsEnabled: Boolean? = null
  var ztoolsConfJson: String? = null

  @DoNotSerialize
  var ztoolsConf: ZtoolsConfig by Delegates.observable(
    ztoolsConfJson?.let { BdtJson.fromJsonToClass(it, ZtoolsConfig::class.java) } ?: ZtoolsConfig()) { _, old, new ->
    if (new == old) return@observable
    ztoolsConfJson = BdtJson.toJson(new)
  }

  // TODO Unused field left for compatibility. Use proxyEnableType instead.
  var enableProxy = false
  var proxyEnableType = ProxyEnableType.DISABLED
  var proxyType = ProxyType.HTTP
  var proxyHost: String = ""
  var proxyPort: Int = 80
  var proxyAuthEnabled = false

  override var tunnel: ConnectionSshTunnelDataLegacy = ConnectionSshTunnelDataLegacy.DEFAULT

  init {
    if (uri.isBlank()) uri = "http://localhost:8080"
    migrateTunnel(this::uri)
  }

  override fun credentialIds() = super.credentialIds() + NGINX_CREDENTIALS_ID + PROXY_CREDENTIALS_ID + CookieAvailable.OAUTH_COOKIE_ID

  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver {
    val driver = ZeppelinDriver(project, this)
    logger.trace("Create new driver: ${System.identityHashCode(driver)}")
    return driver
  }

  @Contract(mutates = "this")
  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  @DoNotSerialize
  val url: String
    get() = formUrl()

  @DoNotSerialize
  var zeppelinUser: Credentials?
    get() = getCredentials()
    set(value) = setCredentials(value)

  @DoNotSerialize
  var httpBasicAuth: Credentials?
    get() = if (enableBasicAuth) getCredentials(NGINX_CREDENTIALS_ID) else null
    set(value) {
      setCredentials(value, NGINX_CREDENTIALS_ID)
      enableBasicAuth = value != null
    }


  @DoNotSerialize
  val cookieStore: ConnectionCookieStore by lazy { ConnectionCookieStore(this) }

  @DoNotSerialize
  val proxySettings: ProxySettings?
    get() = when (proxyEnableType) {
      ProxyEnableType.GLOBAL -> BdtSystemProxy.getIdeaProxySettings(getFullHttpUrl())
      ProxyEnableType.CUSTOM -> {
        val credentials = if (proxyAuthEnabled) getCredentials(PROXY_CREDENTIALS_ID) else null
        ProxySettings(proxyType, proxyHost.trim(), proxyPort, credentials)
      }
      ProxyEnableType.DISABLED -> null
    }

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    ZeppelinConnectionConfigurable(this, project)


  @Suppress("DEPRECATION")
  override fun migrate() {
    if (enableIntellijIntegration == false && isZtoolsEnabled == null) {
      ZtoolsOfferService.offerIds += innerId
    }
    if (enableIntellijIntegration == true) {
      isZtoolsEnabled = true
      val variablesSettings = ZtoolsVariablesSettings(isEnabled = true,
                                                      isOnDemandOnly = enableOnDemandZtools == true,
                                                      sameNoteOnly = ztoolsDfsSameNoteOnly == true)
      val sqlSettings = ZtoolsSqlSettings(isEnabled = true,
                                          collectionStrategy = if (enableOnDemandZtools == true)
                                            ZtoolsSqlCollectStrategy.ONLY_ON_REFRESH
                                          else
                                            ZtoolsSqlCollectStrategy.DEFINED_ON_EACH_RUN_ALL_REFRESH
      )

      ztoolsConf = ZtoolsConfig(variablesSettings = variablesSettings, sqlSettings = sqlSettings)
      enableIntellijIntegration = null
      enableOnDemandZtools = null
      ztoolsSqlSameNoteOnly = null
      ztoolsDfsSameNoteOnly = null
    }
  }

  private fun migrateTunnelInfo() {

  }

  private fun formUrl() = try {
    val rawUri = if (!uri.startsWith("http")) "http://$uri" else uri
    val originUri = URL(rawUri).toURI()
    val resultUrl = URI(originUri.scheme, originUri.userInfo, originUri.host, originUri.port,
                        originUri.path, originUri.query, originUri.fragment).toURL()
    resultUrl.toExternalForm()
  }
  catch (e: Exception) {
    uri
  }

  override fun setCookie(cookies: List<BasicClientCookie>) {
    val jsonCookies = BdtJson.toJson(cookies)
    setCredentials(Credentials("cookie", jsonCookies), CookieAvailable.OAUTH_COOKIE_ID)
  }

  override fun getCookie(): List<BasicClientCookie>? {
    val credentials = getCredentials(CookieAvailable.OAUTH_COOKIE_ID)?.password?.toString() ?: return null
    return BdtJson.fromJsonArray(credentials, BasicClientCookie::class.java)
  }

  override fun cookieOrigins(): Collection<CookieOrigin> = try {
    val uri = URI(formUrl())
    listOf(CookieOrigin(uri.host, uri.port.takeIf { it >= 0 } ?: 0, uri.path, true))
  }
  catch (e: URISyntaxException) {
    emptyList()
  }

  override fun rfsDriverType() = BdtConnectionType.ZEPPELIN

  fun getShowedName(): String = when {
    name.isNotBlank() -> name
    else -> getFullHttpUrl()
  }

  fun getFullHttpUrl(): String = BdtUrlUtils.getFullHttpUrl(url)


  @NlsSafe
  fun getNameWithAddress() = "$name($url)"


  override fun notReloadRequiredKeys() = listOf(ZeppelinSettingsKeys.ZTOOLS_CONFIG, ZeppelinSettingsKeys.ENABLE_ZTOOLS)

  companion object {
    val NGINX_CREDENTIALS_ID = CredentialId("Zeppelin Nginx Credentials")
    val PROXY_CREDENTIALS_ID = CredentialId("Zeppelin Proxy Credentials")
    private val logger = Logger.getInstance(this::class.java)

    fun getUrlToNote(httpUrl: String, noteId: String) = "$httpUrl/#/notebook/$noteId"
    fun getFullWsUrl(url: String): String = "ws" + BdtUrlUtils.getFullHttpUrl(url).removePrefix("http") + "/ws"
  }
}