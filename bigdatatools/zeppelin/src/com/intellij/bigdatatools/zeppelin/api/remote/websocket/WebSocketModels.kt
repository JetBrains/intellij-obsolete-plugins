package com.intellij.bigdatatools.zeppelin.api.remote.websocket

import com.google.gson.JsonObject
import com.intellij.bigdatatools.zeppelin.models.connection.WsMessages
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinCredentials
import com.intellij.bigdatatools.zeppelin.utils.JsonParser

data class ParagraphData(val id: String,
                         val paragraph: String,
                         val title: String?,
                         val params: Map<String, Any?>,
                         val config: Map<String, Any?>) {
  companion object {
    fun create(id: String,
               paragraph: String,
               title: String?,
               params: Map<String, Any?>,
               config: JsonObject) = ParagraphData(id, paragraph, title, params, JsonParser.fromJsonObjectToMap(config))
  }
}

data class CreateParagraphData(val index: Int,
                               val paragraph: String,
                               val title: String?,
                               val params: Map<String, Any?>,
                               val config: Map<String, Any?>) {
  companion object {
    fun create(index: Int,
               paragraph: String,
               title: String?,
               params: Map<String, Any?>,
               config: JsonObject) = CreateParagraphData(index, paragraph, title, params, JsonParser.fromJsonObjectToMap(config))
  }
}

data class WsRequestMessage(val op: String, val data: Any, val ticket: String, val roles: String,
                            val principal: String) {
  companion object {
    fun create(op: String, data: Any, credentials: ZeppelinCredentials): WsRequestMessage {
      return WsRequestMessage(op, data, credentials.ticket,
                              credentials.roles, credentials.principal)
    }
  }
}

data class WsResponseMessage(val op: WsMessages, val data: Any)

/**
 * A web socket listener
 */
interface WebSocketListener {
  fun onMessage(msg: String)
  fun onClose(statusCode: Int?, reason: String?)
  fun onError(throwable: Throwable)
  fun onConnect()
  fun onWebSocketOpen()
  fun onWebsocketPong()
  fun onSendMessage()
  fun onSendPing()
}