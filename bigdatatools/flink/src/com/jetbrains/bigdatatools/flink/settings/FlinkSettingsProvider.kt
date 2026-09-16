package com.jetbrains.bigdatatools.flink.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider

class FlinkSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.FLINK

  override fun createConnectionGroups(): List<ConnectionGroup> {
    return listOf(FlinkConnectionGroup())
  }
}