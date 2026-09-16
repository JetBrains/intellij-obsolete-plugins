package com.intellij.bigdatatools.zeppelin.components.service

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger

class ZeppelinInstanceConnectionLogger(private val connection: ZeppelinInstanceCachedConnection) : Disposable {
  private val config = connection.config

  private val listener = object : ZeppelinConnectionListener {
    override fun onConnected() {
      logger.info("Successfully connected to ${config.getShowedName()} (${config.getFullHttpUrl()})")
    }

    override fun onConnectionError(throwable: Throwable) {
      logger.warn("Connection error to ${config.getShowedName()} (${config.getFullHttpUrl()})", throwable)
    }

    override fun onDisconnected(statusCode: Int?, reason: String?) {
      logger.info("Zeppelin Root Connection closed ${config.getShowedName()} (${config.getFullHttpUrl()}) " +
                  "Status code: $statusCode, reason: $reason")
    }
  }

  init {
    connection.addListener(listener)
  }

  override fun dispose() {
    connection.removeListener(listener)
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}