package com.jetbrains.spark.monitoring.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider

class SparkSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.SPARK
  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(SparkConnectionGroup())
}
