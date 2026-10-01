package com.intellij.aiplayground.ollama

import com.intellij.util.net.ProxyCredentialStore
import com.intellij.util.net.ProxySettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders.ProxyAuthorization
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class PlaygroundHttpClientTimeouts(
  val connect: Duration,
  val request: Duration,
  val socket: Duration = request,
  @Suppress("unused")
  val sseRequest: Duration = Duration.Companion.INFINITE,  // hardcoded in ai.grazie.utils.http.JsonKtorHTTPClient, not changeable
  val sseSocket: Duration = socket * 2,
) {
  TimeoutShort(connect = 10.seconds, request = 15.seconds),
  TimeoutNormal(connect = 10.seconds, request = 30.seconds),
  TimeoutLong(connect = 10.seconds, request = 60.seconds),
  // Note that because of the socket timeout, we still make sure that
  // we receive each response chunk during streaming within a reasonable time.
  TimeoutInfinite(connect = 10.seconds, request = Duration.INFINITE, socket = 30.seconds),
}

object PlaygroundKtorHttpClientUtil {
  fun httpClient(): HttpClient {
    val timeouts = PlaygroundHttpClientTimeouts.TimeoutInfinite

    return HttpClient(Java) {
      install(HttpTimeout) {
        requestTimeoutMillis = timeouts.request.inWholeMilliseconds
        connectTimeoutMillis = timeouts.connect.inWholeMilliseconds
        socketTimeoutMillis = timeouts.socket.inWholeMilliseconds
      }

      install(ProxyAuthenticationPlugin)
    }
  }

  /**
   * CommonProxy.getAuthenticator() cannot be used here because
   * Java Http client erases (see JDK-8326949) `Authorization` header
   * which is used by [ai.grazie.api.gateway.client.api.AuthAPIClient.register].
   * Notably, the default `jdk.http.auth.tunneling.disabledSchemes` configuration disables Basic authorization.
   * However, to enable Basic authorization, `VmOptionsGenerator` sets it to an empty value for distribution versions,
   * but when launching from the source you have to manually set it in the run configuration.
   */
  private val ProxyAuthenticationPlugin = createClientPlugin("ProxyAuthenticationPlugin") {
    onRequest { request, _ ->
      val credentials = ProxyCredentialStore.getInstance().getCredentials(ProxySettings.getInstance().getProxyConfiguration())
      if (credentials?.userName != null) {
        val proxyLogin = credentials.userName
        val proxyPassword = credentials.getPasswordAsString()
        val token = Base64.getEncoder().encodeToString("${proxyLogin}:${proxyPassword}".toByteArray())
        request.headers.append(ProxyAuthorization, "Basic $token")
      }
    }
  }
}
