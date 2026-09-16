package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup

class ArbitraryClusterSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.ARBITRARY_CLUSTER
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.SPARK

  init {
    init()

    registryEvent(ArbitraryClusterSettingsCustomizer::sshComponent)
    registryEvent(ArbitraryClusterSettingsCustomizer::nameField)
    registryCheckboxEvent(ArbitraryClusterSettingsCustomizer::sparkMonitoringConnection)
    registryCheckboxEvent(ArbitraryClusterSettingsCustomizer::sftpConnection)
  }

  object Util {
    fun getInstance(): ArbitraryClusterSettingsCollector = getInstance(BdtConnectionType.ARBITRARY_CLUSTER) as?
                                                             ArbitraryClusterSettingsCollector ?: ArbitraryClusterSettingsCollector()
  }
}
