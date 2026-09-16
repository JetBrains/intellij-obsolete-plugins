package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class ZeppelinInstanceStatisticCollector(val project: Project?, val connection: ZeppelinInstanceCachedConnection) : Disposable {
  private val listener = object : ZeppelinConnectionListener {
    override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) =
      ZeppelinInterpreterUsageCollector.collectInterpreterSettings(project, connection.getContext(), interpreterSettings)

    override fun updateNotebookList(notebooks: List<NotebookInfo>) =
      ZeppelinInstanceUsageCollector.foldersUpdateEvent.log(project,
                                                            value1 = connection.getContext().zeppelinVersion.toStatisticsString() as String,
                                                            value2 = notebooks.size)

    override fun onConnected() = ZeppelinInstanceUsageCollector.connectedEvent.log(project,
                                                                                   value1 = connection.getContext().zeppelinVersion.toStatisticsString() as String)
  }

  init {
    connection.addListener(listener)
  }

  override fun dispose() {
    connection.removeListener(listener)
  }
}