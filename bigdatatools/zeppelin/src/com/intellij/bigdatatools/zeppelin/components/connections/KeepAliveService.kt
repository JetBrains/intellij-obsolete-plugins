package com.intellij.bigdatatools.zeppelin.components.connections

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.openapi.Disposable
import com.jetbrains.bigdatatools.common.util.SimpleRfsUpdater

internal class KeepAliveService(private val connection: ZeppelinConnection) : Disposable {

  init {
    SimpleRfsUpdater(
      disposable = this,
      delay = ZeppelinTimeouts.IDLE_TIMEOUT.toInt() / 2,
      runOnlyInActiveFrame = false
    ).scheduleUpdate {
      pingConnection()
      true
    }
  }

  override fun dispose() {}


  private fun pingConnection() {
    if (!connection.isConnected())
      return

    connection.api.pingZeppelin()
  }
}