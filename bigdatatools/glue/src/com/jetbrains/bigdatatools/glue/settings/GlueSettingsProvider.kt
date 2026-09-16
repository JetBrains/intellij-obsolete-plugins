package com.jetbrains.bigdatatools.glue.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider

class GlueSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.METASTORE_CORE
  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(GlueConnectionGroup())
}