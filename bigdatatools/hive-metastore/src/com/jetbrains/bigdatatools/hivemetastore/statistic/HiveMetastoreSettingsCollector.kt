package com.jetbrains.bigdatatools.hivemetastore.statistic

import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastorePropertySource
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreSettingsCustomizer

class HiveMetastoreSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.HIVE
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.METASTORE

  init {
    init()

    registryEvent(HiveMetastoreSettingsCustomizer::url)
    registryEvent(HiveMetastoreSettingsCustomizer::nameField)
    registryStringEnumEvent(HiveMetastoreSettingsCustomizer::sourceTypeChooser, HiveMetastorePropertySource.entries.map { it.id })
    registryEvent(HiveMetastoreSettingsCustomizer::configFolder)
    registryEvent(HiveMetastoreSettingsCustomizer::propertiesEditor)
    registryEvent(HiveMetastoreSettingsCustomizer::databasePatternField)
    registryEvent(HiveMetastoreSettingsCustomizer::tablePatternField)
    registryCheckboxEvent(HiveMetastoreSettingsCustomizer::useKerberosTicketCache)
    registryEvent(HiveMetastoreSettingsCustomizer::saslPrincipal)
    registryEvent(HiveMetastoreSettingsCustomizer::saslKeytab)

    registryEvent(HiveMetastoreSettingsCustomizer::noneAuthType)
    registryEvent(HiveMetastoreSettingsCustomizer::kerberosAuthType)

    registryEvent(HiveMetastoreSettingsCustomizer::tunnelField)
    registryCheckboxEvent(HiveMetastoreSettingsCustomizer::enableTunnelField)
  }

  object Util {
    fun getInstance(): HiveMetastoreSettingsCollector =
      getInstance(BdtConnectionType.HIVE) as? HiveMetastoreSettingsCollector ?: HiveMetastoreSettingsCollector()
  }
}
