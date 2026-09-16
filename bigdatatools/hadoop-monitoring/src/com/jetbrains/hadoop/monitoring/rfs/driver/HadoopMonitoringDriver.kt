package com.jetbrains.hadoop.monitoring.rfs.driver

import com.intellij.bigdatatools.hadoopMonitoring.icons.BigdatatoolsHadoopMonitoringIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.rfs.MonitoringDriver
import com.jetbrains.hadoop.monitoring.data.HadoopDataManager
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.toolwindow.HadoopToolWindowController

class HadoopMonitoringDriver(project: Project?,
                             override val connectionData: HadoopConnectionData,
                             testConnection: Boolean) : MonitoringDriver(project, testConnection) {
  override val dataManager = HadoopDataManager(project, connectionData)
  override val icon = BigdatatoolsHadoopMonitoringIcons.ToolWindowHadoop

  override fun getController(project: Project): HadoopToolWindowController = HadoopToolWindowController.getInstance(project)

  init {
    Disposer.register(this, dataManager)
  }

  override fun dispose() {}
}