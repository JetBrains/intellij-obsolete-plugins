package com.intellij.bigdatatools.databricks.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider

class DatabricksSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.DATABRICKS

  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(DatabricksConnectionGroup())
}