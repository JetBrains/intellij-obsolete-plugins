package com.intellij.bigdatatools.zeppelin.api.remote.websocket

import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.nv.NvWebSocketClient
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.okhttp.OkHttpWebSocketClient
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.Disposable
import org.apache.http.client.CookieStore

interface WebSocketClient : Disposable {
  val url: String
  fun connect()
  fun close(statusCode: Int?, reason: String?)
  fun sendMessage(requestMessage: WsRequestMessage)
  fun isConnected(): Boolean

  companion object {
    fun createWebSocketClient(config: ZeppelinConnectionData, url: String, cookieStore: CookieStore, basicAuthCredentials: Credentials?, proxySettings: ProxySettings?, webSocketListener: WebSocketListener): WebSocketClient {
      return if (proxySettings?.type != ProxyType.SOCKS)
        NvWebSocketClient(config, url, cookieStore, basicAuthCredentials, proxySettings, webSocketListener)
      else
        OkHttpWebSocketClient(config, url, cookieStore, basicAuthCredentials, proxySettings, webSocketListener)
    }
  }
}