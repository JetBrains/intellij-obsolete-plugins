package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider

class ArbitrarySparkSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.SPARK
  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(ArbitraryClusterConnectionGroup())
}
