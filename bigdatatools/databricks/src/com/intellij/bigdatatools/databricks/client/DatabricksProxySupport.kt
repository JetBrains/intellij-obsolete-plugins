package com.intellij.bigdatatools.databricks.client

import com.databricks.sdk.core.DatabricksConfig
import com.databricks.sdk.core.ProxyConfig
import com.databricks.sdk.core.commons.CommonsHttpClient
import com.intellij.bigdatatools.coreUi.connection.BdtSystemProxy
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object DatabricksProxySupport {
  fun configureHttpClient(config: DatabricksConfig, url: String) {
    val proxySettings = getHttpProxySettings(url) ?: return
    val proxyConfig = ProxyConfig()
      .setHost(proxySettings.host)
      .setPort(proxySettings.port)

    val userName = proxySettings.credentials?.userName?.takeIf { it.isNotBlank() }
    if (userName != null) {
      proxyConfig
        .setUsername(userName)
        .setPassword(proxySettings.credentials?.getPasswordAsString())
        .setProxyAuthType(ProxyConfig.ProxyAuthType.BASIC)
    }

    config.httpClient = CommonsHttpClient.Builder()
      .withDatabricksConfig(config)
      .withProxyConfig(proxyConfig)
      .build()
  }

  fun proxyEnvironment(url: String?): Map<String, String> {
    val proxyUrl = url?.let(::getHttpProxySettings)?.let(::toProxyUrl) ?: return emptyMap()
    return mapOf(
      "http_proxy" to proxyUrl,
      "https_proxy" to proxyUrl,
      "HTTP_PROXY" to proxyUrl,
      "HTTPS_PROXY" to proxyUrl,
    )
  }

  private fun getHttpProxySettings(url: String): ProxySettings? {
    val proxySettings = BdtSystemProxy.getIdeaProxySettings(url) ?: return null
    return proxySettings.takeIf { it.type == ProxyType.HTTP }
  }

  private fun toProxyUrl(proxySettings: ProxySettings): String {
    val protocol = runCatching { URL(proxySettings.host).protocol }.getOrNull() ?: "http"
    val host = proxySettings.host.removePrefix("$protocol://")
    val credentials = proxySettings.credentials?.userName?.takeIf { it.isNotBlank() }?.let { userName ->
      buildString {
        append(URLEncoder.encode(userName, StandardCharsets.UTF_8))
        proxySettings.credentials?.getPasswordAsString()?.let { password ->
          append(':')
          append(URLEncoder.encode(password, StandardCharsets.UTF_8))
        }
        append('@')
      }
    }.orEmpty()
    return "$protocol://$credentials$host:${proxySettings.port}"
  }
}
