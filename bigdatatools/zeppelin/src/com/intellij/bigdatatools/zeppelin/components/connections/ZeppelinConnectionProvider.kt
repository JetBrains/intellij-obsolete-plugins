package com.intellij.bigdatatools.zeppelin.components.connections

interface ZeppelinConnectionProvider {
  /**
   * Check connection to Zeppelin
   */
  fun isConnected(): Boolean
  fun addListener(listener: ZeppelinConnectionListener)
  fun removeListener(listener: ZeppelinConnectionListener)
}