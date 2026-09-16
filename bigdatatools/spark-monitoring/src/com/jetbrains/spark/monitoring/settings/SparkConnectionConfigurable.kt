package com.jetbrains.spark.monitoring.settings

import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTesting

class SparkConnectionConfigurable(connectionData: SparkConnectionData, project: Project) :
  ConnectionConfigurable<SparkConnectionData, SparkSettingsCustomizer>(connectionData, project, BigdatatoolsSparkMonitoringIcons.Spark) {

  override fun getHelpTopic() = "big.data.tools.spark"

  override fun createSettingsCustomizer() = SparkSettingsCustomizer(project, connectionData, disposable, coroutineScope)

  override fun createConnectionTesting(): ConnectionTesting<SparkConnectionData> =
    SparkConnectionTestingBase(project, settingsCustomizer)
}