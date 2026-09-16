package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider

class DataprocSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.METASTORE_CORE

  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(DataprocConnectionGroup())
}