package com.intellij.bigdatatools.zeppelin.components.connections

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.util.BdtUrlUtils
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinApi
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinClosedConnectionException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinWebSocketAPI
import com.intellij.bigdatatools.zeppelin.api.remote.rest.ZeppelinRestApi
import com.intellij.bigdatatools.zeppelin.api.remote.rest.ZeppelinRestClient
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketClient
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WebSocketListener
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WsResponseMessage
import com.intellij.bigdatatools.zeppelin.models.ParseException
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.tunnel.UriTunnelHandler
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.DriverConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FailedConnectionStatus
import java.util.concurrent.atomic.AtomicBoolean

internal class ZeppelinClient(connectionData: ZeppelinConnectionData,
                              val connectionNotifier: ZeppelinConnectionNotifier,
                              val tunnel: UriTunnelHandler?,
                              val addDebugInfo: () -> String) : Disposable {
  private val isConnectingInner = AtomicBoolean(false)
  private val isDisconnected = AtomicBoolean(false)

  private val webSocketListener: WebSocketListener = createWsListener()

  private val zeppelinRestAPI: ZeppelinRestApi
  private val webSocketClient: WebSocketClient
  private val zeppelinWebSocketAPI: ZeppelinWebSocketAPI
  val api: ZeppelinApi

  private var error: Throwable? = null

  init {
    val tunneledUri = tunnel?.tunnelledUri

    val webSocketClientUri = ZeppelinConnectionData.getFullWsUrl(tunneledUri ?: connectionData.url)
    val restClientUri = BdtUrlUtils.getFullHttpUrl(tunneledUri ?: connectionData.url)

    val cookieStore = connectionData.cookieStore
    val httpBasicAuth = connectionData.httpBasicAuth
    val proxySettings = connectionData.proxySettings
    val restClient = ZeppelinRestClient(connectionData, cookieStore, httpBasicAuth, proxySettings, restClientUri)
    zeppelinRestAPI = ZeppelinRestApi(restClient)

    webSocketClient = WebSocketClient.createWebSocketClient(connectionData, webSocketClientUri, cookieStore, httpBasicAuth, proxySettings, webSocketListener)
    zeppelinWebSocketAPI = ZeppelinWebSocketAPI(webSocketClient)
    api = ZeppelinApi(zeppelinWebSocketAPI, zeppelinRestAPI, connectionData)

    Disposer.register(this, webSocketClient)
    Disposer.register(this, restClient)

    try {
      isConnectingInner.set(true)
      api.connect()
      error = null
    }
    catch (e: Throwable) {
      error = e
      closeConnection(null, "Exception during connect")
      Disposer.dispose(this)
      throw e
    }
    finally {
      isConnectingInner.set(false)
    }
  }

  override fun dispose() {
    tunnel?.let { Disposer.dispose(it) }
  }

  fun disconnect(statusCode: Int? = null, reason: String? = null) {
    try {
      if (isDisconnected.get()) return
      isDisconnected.set(true)

      closeConnection(statusCode, reason)
      onConnectionClose(statusCode, reason)
    }
    catch (e: Exception) {
      logger.warn("Cannot destroy the connection.", e)
    }
  }

  fun onConnectionClose(statusCode: Int?, reason: String?) {
    if (isConnectingInner.get()) return

    logger.info("Zeppelin CONNECTION closed statusCode $statusCode, reason: $reason  ${addDebugInfo()}")
    connectionNotifier.notifyOnClose(statusCode, reason)

    error = ZeppelinClosedConnectionException(statusCode, reason)
  }

  private fun closeConnection(statusCode: Int?, reason: String?) {
    api.close(statusCode, reason)
    isConnectingInner.set(false)
  }

  fun isConnected(): Boolean = !isConnectingInner.get() && webSocketClient.isConnected()
  fun getConnectionStatus(): DriverConnectionStatus = when {
    webSocketClient.isConnected() -> ConnectedConnectionStatus
    else -> FailedConnectionStatus(error ?: BdtConnectionException())
  }

  private fun createWsListener() = object : WebSocketListener {
    override fun onSendMessage() = logger.trace("Zeppelin WS SEND MESSAGE ${addDebugInfo()}")

    override fun onSendPing() = logger.trace("Zeppelin WS SEND PING ${addDebugInfo()}")

    override fun onConnect() {
      isDisconnected.set(false)
      logger.info("Zeppelin WS CONNECTED ${addDebugInfo()}")
    }

    override fun onClose(statusCode: Int?, reason: String?) {
      logger.info("Zeppelin WS CLOSED, statusCode: ${statusCode}, reason: ${reason} ${addDebugInfo()} ")
      if (!isDisconnected.get()) {
        isDisconnected.set(true)

        onConnectionClose(statusCode, reason)
      }
    }

    override fun onWebSocketOpen() = logger.trace("Zeppelin WS OPEN  ${addDebugInfo()}")

    override fun onWebsocketPong() = logger.trace("Zeppelin WS PONG  ${addDebugInfo()}")

    override fun onError(throwable: Throwable) {
      logger.info("Zeppelin WS ERROR ${addDebugInfo()}", throwable)
      if (isConnected()) connectionNotifier.notifyOnError(throwable)
    }

    override fun onMessage(msg: String) {
      val message = try {
        JsonParser.fromStringObject(msg, WsResponseMessage::class.java)
      }
      catch (e: ParseException) {
        logger.error("Cannot parse web socket message, Full message:\n$msg", e)
        return
      }
      logger.trace("Zeppelin WS MESSAGE RECEIVED. Code: ${message.op} ${addDebugInfo()}")

      if (!isConnected()) return
      connectionNotifier.notifyOnMessage(message, api.innerZeppelinInfo)
    }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}