package com.intellij.bigdatatools.zeppelin.components.service

import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionProvider
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.bigdatatools.common.util.invokeLater

class ZeppelinServerErrorNotifier(project: Project?,private val zeppelinConnectionProvider: ZeppelinConnectionProvider): Disposable {
  val listener = object: ZeppelinConnectionListener {
    @Suppress("HardCodedStringLiteral")
    override fun onServerError(info: String) {
      val title = ZepMessagesBundle.message("server.error.title")
      invokeLater {
        Messages.showErrorDialog(project, info, title)
      }
    }
  }

  init {
    zeppelinConnectionProvider.addListener(listener)
  }

  override fun dispose() {
    zeppelinConnectionProvider.removeListener(listener)
  }
}