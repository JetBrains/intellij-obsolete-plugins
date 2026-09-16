package com.jetbrains.hadoop.monitoring.statistics

import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup
import com.jetbrains.hadoop.monitoring.settings.HadoopSettingsCustomizer

class HadoopSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.YARN
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.METASTORE

  init {
    init()

    registryEvent(HadoopSettingsCustomizer::nameField)
    registryEvent(HadoopSettingsCustomizer::url)
    registryEvent(HadoopSettingsCustomizer::tunnelField)
    registryCheckboxEvent(HadoopSettingsCustomizer::enableTunnelField)

    // Basic auth settings
    registryCheckboxEvent(HadoopSettingsCustomizer::enableBasicAuthCheckbox)
    registryEvent(HadoopSettingsCustomizer::basicAuthLoginField)
    registryEvent(HadoopSettingsCustomizer::basicAuthPasswordField)

    // Proxy settings
    registryEvent(HadoopSettingsCustomizer::proxyLoginField)
    registryEvent(HadoopSettingsCustomizer::proxyPasswordField)
    registryEvent(HadoopSettingsCustomizer::proxyHostField)
    registryEvent(HadoopSettingsCustomizer::proxyPortField)
    registryStringEnumEvent(HadoopSettingsCustomizer::proxyEnableComboBox, ProxyEnableType.entries.map { it.name })
    registryEvent(HadoopSettingsCustomizer::proxyTypeComboBox)
    registryCheckboxEvent(HadoopSettingsCustomizer::proxyAuthEnabledCheckbox)

    registryEvent(HadoopSettingsCustomizer::sparkMonitoringComboBox)
  }

  object Util {
    fun getInstance(): HadoopSettingsCollector =
      getInstance(BdtConnectionType.YARN) as? HadoopSettingsCollector ?: HadoopSettingsCollector()
  }
}
