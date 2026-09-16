package com.jetbrains.bigdatatools.flink.rfs

import com.intellij.bigdatatools.flink.icons.BigdatatoolsFlinkIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.toolwindow.FlinkMonitoringToolWindowController
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import javax.swing.Icon

class FlinkDriver(override val connectionData: FlinkConnectionData,
                  project: Project?,
                  testConnection: Boolean) : MonitoringDriver(project, testConnection) {
  override val dataManager: FlinkDataManager = FlinkDataManager(project, connectionData,
                                                                FlinkToolWindowSettings.getInstance())
  override val presentableName: String = connectionData.name
  override val icon: Icon = BigdatatoolsFlinkIcons.Flink

  init {
    Disposer.register(this, dataManager)
  }

  override fun dispose() {}

  override fun getController(project: Project) = FlinkMonitoringToolWindowController.getInstance(project)
}