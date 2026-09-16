package com.jetbrains.bigdatatools.flink.statistic

import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup
import com.jetbrains.bigdatatools.flink.settings.FlinkSettingsCustomizer

class FlinkSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.FLINK
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.FLINK

  init {
    init()

    registryEvent(FlinkSettingsCustomizer::nameField)
    registryEvent(FlinkSettingsCustomizer::url)
    registryEvent(FlinkSettingsCustomizer::tunnelField)
    registryCheckboxEvent(FlinkSettingsCustomizer::enableTunnelField)

    // Basic auth settings
    registryCheckboxEvent(FlinkSettingsCustomizer::enableBasicAuthCheckbox)
    registryEvent(FlinkSettingsCustomizer::basicAuthLoginField)
    registryEvent(FlinkSettingsCustomizer::basicAuthPasswordField)

    // Proxy settings
    registryEvent(FlinkSettingsCustomizer::proxyLoginField)
    registryEvent(FlinkSettingsCustomizer::proxyPasswordField)
    registryEvent(FlinkSettingsCustomizer::proxyHostField)
    registryEvent(FlinkSettingsCustomizer::proxyPortField)
    registryStringEnumEvent(FlinkSettingsCustomizer::proxyEnableComboBox, ProxyEnableType.entries.map { it.name })
    registryEvent(FlinkSettingsCustomizer::proxyTypeComboBox)
    registryCheckboxEvent(FlinkSettingsCustomizer::proxyAuthEnabledCheckbox)
  }

  object Util {
    fun getInstance(): FlinkSettingsCollector = getInstance(BdtConnectionType.FLINK) as? FlinkSettingsCollector ?: FlinkSettingsCollector()
  }
}
