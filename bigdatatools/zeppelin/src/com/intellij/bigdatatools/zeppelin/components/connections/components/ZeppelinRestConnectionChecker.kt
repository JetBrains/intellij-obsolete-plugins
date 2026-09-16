package com.intellij.bigdatatools.zeppelin.components.connections.components

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnection
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.util.SimpleRfsUpdater

class ZeppelinRestConnectionChecker(private val zepConnection: ZeppelinConnection) : Disposable {
  val api get() = zepConnection.api

  init {
    SimpleRfsUpdater(this, delay = 4000).scheduleUpdate {
      if (!zepConnection.isConnected())
        return@scheduleUpdate true
      try {
        api.serverInfo()
      }
      catch (t: Throwable) {
        zepConnection.disconnect(reason = "Rest request return error: $t")
      }
      true
    }
  }

  override fun dispose() {}
}