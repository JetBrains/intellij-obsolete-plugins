package com.jetbrains.spark.monitoring.statistics

import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup
import com.jetbrains.spark.monitoring.settings.SparkSettingsCustomizer

class SparkMonitoringSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.SPARK_MONITORING
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.SPARK

  init {
    init()

    registryEvent(SparkSettingsCustomizer::nameField)
    registryEvent(SparkSettingsCustomizer::url)
    registryEvent(SparkSettingsCustomizer::tunnelField)
    registryCheckboxEvent(SparkSettingsCustomizer::enableTunnelField)

    // Basic auth settings
    registryCheckboxEvent(SparkSettingsCustomizer::enableBasicAuthCheckbox)
    registryEvent(SparkSettingsCustomizer::basicAuthLoginField)
    registryEvent(SparkSettingsCustomizer::basicAuthPasswordField)

    // Proxy settings
    registryEvent(SparkSettingsCustomizer::proxyLoginField)
    registryEvent(SparkSettingsCustomizer::proxyPasswordField)
    registryEvent(SparkSettingsCustomizer::proxyHostField)
    registryEvent(SparkSettingsCustomizer::proxyPortField)
    registryStringEnumEvent(SparkSettingsCustomizer::proxyEnableComboBox, ProxyEnableType.entries.map { it.name })
    registryEvent(SparkSettingsCustomizer::proxyTypeComboBox)
    registryCheckboxEvent(SparkSettingsCustomizer::proxyAuthEnabledCheckbox)
  }

  object Util {
    fun getInstance(): SparkMonitoringSettingsCollector = getInstance(BdtConnectionType.SPARK_MONITORING) as?
                                                            SparkMonitoringSettingsCollector ?: SparkMonitoringSettingsCollector()
  }
}
