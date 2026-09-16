package com.jetbrains.hadoop.monitoring.settings

import com.intellij.bigdatatools.hadoopMonitoring.icons.BigdatatoolsHadoopMonitoringIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTesting

class HadoopConnectionConfigurable(connectionData: HadoopConnectionData, project: Project) :
  ConnectionConfigurable<HadoopConnectionData, HadoopSettingsCustomizer>(connectionData, project, BigdatatoolsHadoopMonitoringIcons.ToolWindowHadoop) {

  override fun createSettingsCustomizer() = HadoopSettingsCustomizer(project, connectionData, disposable, coroutineScope)

  override fun createConnectionTesting(): ConnectionTesting<HadoopConnectionData> =
    HadoopConnectionDataRfsConnectionTestingBase(project, settingsCustomizer)
}