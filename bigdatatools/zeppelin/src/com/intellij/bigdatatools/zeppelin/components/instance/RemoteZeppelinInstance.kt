package com.intellij.bigdatatools.zeppelin.components.instance

import com.intellij.bigdatatools.zeppelin.components.instance.service.ZeppelinFileSystem
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInstanceFileCloser
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsManager
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinServerErrorNotifier
import com.intellij.bigdatatools.zeppelin.dependency.ZeppelinDependencyManager
import com.intellij.bigdatatools.zeppelin.statistics.ZeppelinInstanceStatisticCollector
import com.intellij.bigdatatools.zeppelin.utils.PluginSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

class RemoteZeppelinInstance(val project: Project?, connectionManager: ZeppelinConnectionManager) : Disposable {
  val config = connectionManager.config
  private val connection = connectionManager.instanceConnection

  val fileSystem = ZeppelinFileSystem(project, connection)
  val interpreterSettingsManager = ZeppelinInterpreterSettingsManager(connection)

  val dependencyManager = if (PluginSupport.isJavaSupport())
    ZeppelinDependencyManager(project, connection)
  else
    null


  init {
    Disposer.register(this, fileSystem)
    Disposer.register(this, ZeppelinServerErrorNotifier(project, connection))
    Disposer.register(this, ZeppelinInstanceStatisticCollector(project, connection))
    Disposer.register(this, ZeppelinInstanceFileCloser(project, connection))
    Disposer.register(this, interpreterSettingsManager)
    dependencyManager?.let { Disposer.register(this, it) }
  }

  override fun dispose() {
  }
}