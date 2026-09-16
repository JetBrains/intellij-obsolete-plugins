package com.intellij.bigdatatools.zeppelin.api.remote.websocket.okhttp

import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinConnectionUtil
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketClient
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketListener
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WsRequestMessage
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.WebSocket
import okio.ByteString
import org.apache.http.client.CookieStore
import org.apache.http.impl.cookie.BasicClientCookie
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.util.Date
import java.util.concurrent.TimeUnit


class OkHttpWebSocketClient(val config: ZeppelinConnectionData,
                            override val url: String,
                            private val cookieStore: CookieStore,
                            private val basicAuthCredentials: com.intellij.credentialStore.Credentials?,
                            val proxySettings: ProxySettings?,
                            private val webSocketListener: WebSocketListener) : WebSocketClient {
  private val client = createHttpClient()

  private var connection: WebSocket? = null
  private var internalListener: OkWsListener? = null

  override fun connect() {
    val okWebSocketListener = OkWsListener()
    val ws = createConnection(okWebSocketListener)
    connection?.cancel()
    connection = ws
    internalListener = okWebSocketListener
    okWebSocketListener.awaitConnection()
  }

  inner class OkWsListener : okhttp3.WebSocketListener() {
    private val startWaiter = BdtAsyncPromise<Boolean>()
    private var connectionError: Throwable? = null

    private var reconnectTriesCount: Int = 0
    var isConnected = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
      reconnectTriesCount = 1

      isConnected = true
      startWaiter.setResult(true)

      webSocketListener.onConnect()
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      onClose(code, reason)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      onClose(code, reason)
    }

    private fun onClose(code: Int, reason: String) {
      if (!isConnected)
        return

      isConnected = false
      reconnectTriesCount--

      webSocketListener.onClose(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      connectionError = t
      startWaiter.setResult(false)

      if (!isConnected) return

      close(null, t.message)
      isConnected = false


      //We need to use silent reconnect try because in some cases with https connection to Zeppelin
      //we have the situation that we cannot receive pong msg from the server
      //to reproduce we need to open 2 notes with long spark tasks (SparkPi enough) and click run All on both notes
      if (t is SocketTimeoutException && reconnectTriesCount > 0) {
        reconnectTriesCount--
        val isReconnected = try {
          connect()
          isConnected
        }
        catch (t2: Throwable) {
          false
        }
        if (!isReconnected) {
          webSocketListener.onError(t)
          webSocketListener.onClose(null, t.message)
        }
        return
      }

      webSocketListener.onError(t)
      webSocketListener.onClose(null, t.message)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      webSocketListener.onMessage(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
    }

    fun awaitConnection() {
      val result = startWaiter.blockingGet((ZeppelinTimeouts.CONNECTION_TIMEOUT).toInt(), TimeUnit.MILLISECONDS) ?: false
      if (!result) {
        throw connectionError ?: Exception("WS connection error")
      }
    }
  }

  private fun createHttpClient(): OkHttpClient {
    val clientBuilder = OkHttpClient.Builder()
      .followSslRedirects(true)
      .followRedirects(true)
      .readTimeout(ZeppelinTimeouts.CONNECTION_TIMEOUT, TimeUnit.SECONDS)
      .connectionPool(connectionPool)
      .connectTimeout(ZeppelinTimeouts.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
      .pingInterval(0, TimeUnit.MILLISECONDS)
      .hostnameVerifier { _, _ -> true }
      .sslSocketFactory(ZeppelinConnectionUtil.getTrustAllSocketFactory(),
                        ZeppelinConnectionUtil.x509TrustAllManager)
    addProxy(clientBuilder)
    addBasicAuth(clientBuilder)
    clientBuilder.cookieJar(object : CookieJar {
      override fun loadForRequest(url: HttpUrl): List<Cookie> = cookieStore.cookies.map { clientCookie ->
        val builder = Cookie.Builder()
        clientCookie.domain?.let { builder.domain(it.removePrefix(".")) }
        clientCookie.name?.let { builder.name(it) }
        clientCookie.value?.let { builder.value(it) }
        clientCookie.path?.let { builder.path(it) }
        clientCookie.expiryDate?.let { builder.expiresAt(it.time) }
        if (clientCookie.isSecure) {
          builder.secure()
        }
        builder.build()
      }

      override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach {
          cookieStore.addCookie(BasicClientCookie(it.name, it.value).apply {
            this.domain = it.domain
            this.path = it.path
            this.expiryDate = Date(it.expiresAt)
            this.isSecure = it.secure
          })
        }
      }
    })

    return clientBuilder.build()
  }

  private fun createConnection(okWebSocketListener: OkWsListener): WebSocket {
    val request = Request.Builder().url(url)
    config.headers?.forEach {
      request.addHeader(it.key, it.value)
    }

    return client.newWebSocket(request.build(), okWebSocketListener)
  }

  private fun addProxy(clientBuilder: OkHttpClient.Builder) {
    val proxies = if (proxySettings != null) {
      val port = proxySettings.port
      val host = proxySettings.host
      val proxyType = if (proxySettings.type == ProxyType.SOCKS) Proxy.Type.SOCKS else Proxy.Type.HTTP
      val proxy = Proxy(proxyType, InetSocketAddress.createUnresolved(host, port))
      listOf(proxy)
    }
    else {
      listOf()
    }.toMutableList()

    clientBuilder.proxySelector(object : ProxySelector() {
      override fun select(uri: URI?): MutableList<Proxy> = proxies
      override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
    })

    addProxyAuth(clientBuilder)
  }

  private fun addBasicAuth(clientBuilder: OkHttpClient.Builder) {
    clientBuilder.authenticator(object : Authenticator {
      override fun authenticate(route: Route?, response: Response): Request {
        if (basicAuthCredentials == null) return response.request
        val okCredentials = Credentials.basic(basicAuthCredentials.userName ?: "", basicAuthCredentials.getPasswordAsString() ?: "")
        return response.request.newBuilder()
          .header("Authorization", okCredentials)
          .build()
      }
    })
  }

  private fun addProxyAuth(clientBuilder: OkHttpClient.Builder) {
    clientBuilder.proxyAuthenticator(object : Authenticator {
      override fun authenticate(route: Route?, response: Response): Request {
        val originalRequest = response.request
        val newRequestBuilder = originalRequest.newBuilder()

        val credentials = proxySettings?.credentials ?: return originalRequest
        val okCredentials = Credentials.basic(credentials.userName ?: "", credentials.getPasswordAsString() ?: "")
        return newRequestBuilder
          .header("Proxy-Authorization", okCredentials)
          .build()
      }
    })
  }

  override fun close(statusCode: Int?, reason: String?) {
    internalListener?.isConnected = false
    connection?.close(statusCode ?: 1000, reason ?: "")
  }

  override fun sendMessage(requestMessage: WsRequestMessage) {
    val msg: String = JsonParser.toJson(requestMessage)
    connection?.send(msg)
  }

  override fun isConnected() = internalListener?.isConnected == true

  override fun dispose() {
    connection?.close(1000, "Disposed")
    client.dispatcher.executorService.shutdown()
  }


  companion object {
    private val connectionPool = ConnectionPool()
  }
}
