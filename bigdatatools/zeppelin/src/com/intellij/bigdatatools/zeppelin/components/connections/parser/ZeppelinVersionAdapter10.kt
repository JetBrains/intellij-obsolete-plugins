package com.intellij.bigdatatools.zeppelin.components.connections.parser

import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo

internal object ZeppelinVersionAdapter10 : ZeppelinVersionAdapter9() {
  override fun isSupport(info: ZeppelinInfo): Boolean = info.versionInt >= 10
}