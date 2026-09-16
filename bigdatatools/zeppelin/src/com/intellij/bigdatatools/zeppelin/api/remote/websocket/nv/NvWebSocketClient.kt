package com.intellij.bigdatatools.zeppelin.api.remote.websocket.nv

import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinConnectionUtil
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketClient
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketListener
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WsRequestMessage
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import org.apache.http.client.CookieStore
import java.net.URI


class NvWebSocketClient(val config: ZeppelinConnectionData,
                        override val url: String,
                        private val cookieStore: CookieStore,
                        private val basicAuthCredentials: com.intellij.credentialStore.Credentials?,
                        val proxySettings: ProxySettings?,
                        val webSocketListener: WebSocketListener) : WebSocketClient {
  private var reconnectTriesCount: Int = 0

  private var connection: WebSocket? = null
  private val factory: WebSocketFactory = WebSocketFactory()

  override fun connect() {
    synchronized(connectionMutex) {
      val ws: WebSocket = createConnection()

      connection = ws
      ws.connect()
    }
  }

  private fun createConnection(): WebSocket {
    setupProxy()
    setupSsl()

    val ws: WebSocket = factory.createSocket(url, 0)
    config.headers?.forEach {
      ws.addHeader(it.key, it.value)
    }
    ws.addHeader("Cookie", cookieStore.cookies.joinToString(separator = "; ") { "${it.name}=${it.value}" })
    setupBasicAuth(ws)

    ws.isMissingCloseFrameAllowed = false
    ws.pingInterval = ZeppelinTimeouts.IDLE_TIMEOUT / 2
    ws.addListener(object : WebSocketAdapter() {
      override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
        reconnectTriesCount = 1
        webSocketListener.onConnect()
      }

      override fun onDisconnected(websocket: WebSocket?,
                                  serverCloseFrame: WebSocketFrame?,
                                  clientCloseFrame: WebSocketFrame?,
                                  closedByServer: Boolean) {
        if (clientCloseFrame?.closeReason == "Pong timeout exceed" && reconnectTriesCount > 0) {
          reconnectTriesCount--
          //It is awful hack to run silent reconnect  but I have no choice because in this thread the nv library return
          //Socket connection error, but I hope this method will be called not so often
          // (just with https connections with proxy and rare cases)
          executeOnPooledThread {
            val isReconnectionSuccess =
              try {
                connect()
                true
              }
              catch (t: Throwable) {
                false
              }

            if (!isReconnectionSuccess) {
              webSocketListener.onClose(0, "")
            }
          }
        }

        webSocketListener.onClose(0, "")

        //We need to make silent reconnect as in #OkHttpWebSocketClient
      }

      override fun onError(websocket: WebSocket, cause: WebSocketException) {
        webSocketListener.onError(cause)
      }

      override fun onTextMessage(websocket: WebSocket?, text: String?) {
        text ?: return
        if (websocket?.isOpen == true)
          webSocketListener.onMessage(text)
      }

      override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        webSocketListener.onSendPing()
      }

      override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        webSocketListener.onWebsocketPong()
      }
    })
    return ws
  }

  override fun close(statusCode: Int?, reason: String?) {
    connection?.flush()

    when {
      statusCode != null && reason != null -> connection?.disconnect(statusCode, reason)
      statusCode != null -> connection?.disconnect(statusCode)
      reason != null -> connection?.disconnect(reason)
      else -> connection?.disconnect()
    }
    connection?.socket?.close()
  }

  override fun sendMessage(requestMessage: WsRequestMessage) {
    val msg: String = JsonParser.toJson(requestMessage)
    connection?.sendText(msg)
  }

  override fun isConnected() = connection?.isOpen ?: false

  override fun dispose() {}

  private fun setupSsl() {
    factory.verifyHostname = false
    if (URI(url).scheme == "wss")
      factory.socketFactory = ZeppelinConnectionUtil.getTrustAllSocketFactory()
  }

  private fun setupProxy() {
    val proxySettings = proxySettings ?: return
    assert(proxySettings.type != ProxyType.SOCKS) {
      "Wrong WS library selected for SOCKS proxy"
    }

    factory.proxySettings.host = proxySettings.host
    factory.proxySettings.port = proxySettings.port
    val credentials = proxySettings.credentials
    if (credentials != null) {
      factory.proxySettings.setCredentials(credentials.userName, credentials.getPasswordAsString())
    }
  }

  private fun setupBasicAuth(ws: WebSocket) {
    if (basicAuthCredentials != null) {
      ws.setUserInfo(basicAuthCredentials.userName, basicAuthCredentials.getPasswordAsString())
    }
  }

  companion object {
    val connectionMutex = Any()
  }
}